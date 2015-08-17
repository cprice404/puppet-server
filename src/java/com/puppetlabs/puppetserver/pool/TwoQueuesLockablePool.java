package com.puppetlabs.puppetserver.pool;

import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReentrantLock;

/**
 * An implementation of LockablePool that uses two queues--a "live"
 * queue and a "locked" queue--to restrict access to the items when
 * a lock is requested / held.
 */
public class TwoQueuesLockablePool<E> implements LockablePool<E> {
    // the queue that consumers will borrow from under normal circumstances
    private final LinkedBlockingDeque<E> live_queue;
    // a queue where we will temporarily stash instances when we're locking/locked,
    // so that they're not available to be borrowed.
    private final LinkedBlockingDeque<E> locked_queue;

    // And now, the suck begins. :(

    // we need a lock that allows us to prevent the "register" method
    // from trying to add instances to the pool while the lock is held,
    // because the "lock" method can't be implemented without knowing
    // how many registered instances we have up front.
    private final ReentrantLock sizeLock = new ReentrantLock();

    // we need another lock that actually represents the locked/unlocked
    // state of the pool overall.
    private final ReentrantLock writeLock = new ReentrantLock();

    // and, lastly, we need a way for the "lock" method to know when all
    // of the instances are back in the pool, because we need for the "lock"
    // method to block until then.
    private final ReducibleSemaphore semaphore = new ReducibleSemaphore(0);

    // a counter to track how many registered instances we have.  This might
    // go away if we combine this with the "register" method we have in the master
    // branch right now, where we are keeping a Set of the registered instances.
    private volatile int count = 0;

    public TwoQueuesLockablePool(int capacity) {
        live_queue = new LinkedBlockingDeque<>(capacity);
        locked_queue = new LinkedBlockingDeque<>(capacity);
    }

    public void register(E e) throws InterruptedException {
        // lock to make sure we're the only thread mucking with the size of the pool.
        sizeLock.lock();
        try {
            live_queue.putLast(e);
            count++;
            // increase the value of the semaphore to represent that there is one
            // more instance available.
            semaphore.release();
        } finally {
            sizeLock.unlock();
        }
    }

    public E borrowItem() throws InterruptedException {
        E e;
        // this trick is how we support re-entrancy; we need for it to be possible
        // for the thread that is holding the lock to be able to borrow an instance,
        // so, if we're that thread, we cheat and borrow from the locked queue
        // instead of the live queue.
        if (writeLock.isHeldByCurrentThread()) {
            e = locked_queue.takeFirst();
        } else {
            // all the rest of you suckers are stuck waiting for the live queue
            // to be re-populated
            e = live_queue.takeFirst();
        }
        // in either case, if we've borrowed an instance, we need to decrease
        // the value in the semaphore to reflect that there is one less instance
        // available.  Note that we're calling the non-blocking 'reduce' instead
        // of 'acquire'; we aren't using the semaphore to gate access to the queue, just using it
        // as a mechanism for tracking the count.
        semaphore.reduce(1);
        return e;
    }

    public void returnItem(E e) throws InterruptedException {
        // here's how we give priority to the "lock" thread over any other threads
        // that are trying to do a borrow: if a return happens while the lock
        // is held, we return the instance to the "locked" queue rather than the
        // live queue.  Other threads that are waiting for a borrow will be blocked
        // on a "take" from the live queue, which will be empty by the time the
        // "lock" method returns.
        //
        // A subtle note here; this method is the reason we need the
        // separate 'sizeLock'.  If we tried to use a single lock for both the
        // "register" method and the "lock" method, then, if "register" was holding
        // the lock while an instance was returned, it'd incorrectly get stuck
        // in the locked queue even though the pool wasn't really "locked".
        if (writeLock.isLocked()) {
            locked_queue.putFirst(e);
        } else {
            // suckers go here
            live_queue.putFirst(e);
        }
        // and increase the counter on the semaphore to make it clear that there's
        // another instance back in the pool.
        semaphore.release();
    }

    public synchronized void lock() throws InterruptedException {
        // first we have to grab the size lock, to make sure that there is no
        // "register" in progress, and thus guarantee that we know what the pool
        // size/count is.
        sizeLock.lock();
        // now we grab the "real" pool lock, which will cause any borrowed
        // instances to start being returned to the "locked" queue instead of the
        // "live" queue.
        writeLock.lock();
        // at this point we know that no more instances will be returned to
        // the "live" queue, so we can go ahead and grab all of the instances
        // that are still in there (if any) and move them over to the "locked"
        // queue.  This will prevent any new borrow calls from getting priority
        // over the lock.
        while (live_queue.size() != 0) {
            E e = live_queue.takeLast();
            locked_queue.putFirst(e);
        }
        // now we block on the semaphore, waiting for all of the instances to be
        // returned to the pool.  This line is the only reason we need this semaphore
        // at all; I couldn't come up with another way to make this method block
        // waiting for all of the instances to be returned.
        semaphore.acquire(count);

        if (live_queue.size() != 0) {
            throw new IllegalStateException("The live queue should be empty, yo.");
        }
        if (locked_queue.size() != count) {
            throw new IllegalStateException("The locked queue should be full, yo.");
        }
    }

    public synchronized void unlock() throws InterruptedException {
        // assert sizeLock.isHeldByCurrentThread();
        // assert writeLock.isHeldByCurrentThread();

        if (locked_queue.size() != count) {
            throw new IllegalStateException("The locked queue should be full, yo.");
        }

        // we need to unlock this before we allow any instances back into the
        // live queue, otherwise it's possible that a borrow/return could happen
        // before we release this lock, and we'd end up with an instance being
        // returned to the "locked" queue.  It's also important that we release
        // this lock before we release the size lock since that's the order we
        // acquired them in.
        writeLock.unlock();

        // Put all the instances back into the live queue.  Once this starts
        // happening, borrows/returns on other threads will be unblocked
        while (locked_queue.size() != 0) {
            E e = locked_queue.takeLast();
            live_queue.putFirst(e);
        }
        // bump the semaphore back up to reflect the available instances
        semaphore.release(count);

        if (locked_queue.size() != 0) {
            throw new IllegalStateException("The locked queue should be empty, yo.");
        }

        // lastly, unblock calls to "register" (and "lock").
        sizeLock.unlock();


    }

}
