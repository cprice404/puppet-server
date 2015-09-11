package com.puppetlabs.puppetserver.pool;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ReentrantLockLockablePool<E> implements LockablePool<E> {

    // The wait queue
    private final BlockingQueue<E> queue;

    private final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock(true);

    private final AtomicReference<E> lastReturnedItem = new AtomicReference<>();

    public ReentrantLockLockablePool(int capacity) {
        // Need to use a fair blocking queue implementation
        queue = new ArrayBlockingQueue<>(capacity, true);
    }

    public void register(E item) throws InterruptedException {
        // Let’s pretend we already loaned this out
        // We need to get the read lock, which would've been done at loan time under normal circumstances
        // This also prevents us registering during write, which we want in case we need to do some housekeeping
        rwl.readLock().lock();
        // Now return it as any other item...
        returnItem(item);
    }

    public E borrowItem() throws InterruptedException {
        E item;
        if (rwl.isWriteLockedByCurrentThread()) {
            // This is the write lock holder reentering
            // Just give them whatever instance we happened to have seen last
            item = lastReturnedItem.get();
        } else {
            // Enqueue a request for an item
            // This queue is fair and keeps me my place in it
            item = queue.take();
        }
        // We need a read lock to make a loan to ensure no one is writing while
        // also allowing multiple simultaneous loan to complete
        // If there's a write lock already held then all borrowers might get stuck here
        // That's OK! The write lock can still come through this method, take the
        // other code path and get an item in a non-blocking manner
        rwl.readLock().lock();
        return item;
    }

    public void returnItem(E item) throws InterruptedException {
        lastReturnedItem.set(item);
        rwl.readLock().unlock();
        if (!rwl.isWriteLockedByCurrentThread()) {
            queue.put(item);
        }
    }

    public void lock() {
        rwl.writeLock().lock();
    }

    public void unlock() {
        rwl.writeLock().unlock();
    }

}
