package com.puppetlabs.puppetserver.pool;

import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class RRWLLockablePool<E> implements LockablePool<E> {
    private final LinkedBlockingDeque<E> live_queue;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);
    private final AtomicReference<E> lastReturnedItem = new AtomicReference<>();

    public RRWLLockablePool(int size) {
        live_queue = new LinkedBlockingDeque<>(size);
    }

    @Override
    public void register(E e) throws InterruptedException {
        try {
            lock.readLock().lock();
            lastReturnedItem.set(e);
            live_queue.putLast(e);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public E borrowItem() throws InterruptedException {
        E item;
        if (lock.isWriteLockedByCurrentThread()) {
            item = lastReturnedItem.get();
        } else {
            item = live_queue.takeFirst();
        }
        lock.readLock().lock();
        return item;
    }

    @Override
    public void returnItem(E e) throws InterruptedException {
        lastReturnedItem.set(e);
        if (!lock.isWriteLockedByCurrentThread()) {
            live_queue.putFirst(e);
        }
        lock.readLock().unlock();
    }

    @Override
    public void lock() throws InterruptedException {
        lock.writeLock().lock();
    }

    @Override
    public void unlock() throws InterruptedException {
        lock.writeLock().unlock();
    }

}
