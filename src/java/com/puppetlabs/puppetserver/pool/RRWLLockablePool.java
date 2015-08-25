package com.puppetlabs.puppetserver.pool;

import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class RRWLLockablePool<E> implements LockablePool<E> {
    private final LinkedBlockingDeque<E> live_queue;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);

    public RRWLLockablePool(int size) {
        live_queue = new LinkedBlockingDeque<>(size);
    }

    @Override
    public void register(E e) throws InterruptedException {
        live_queue.putLast(e);
    }

    @Override
    public E borrowItem() throws InterruptedException {
        if (lock.isWriteLockedByCurrentThread()) {
            throw new IllegalStateException("The current implementation has a risk of deadlock if you attempt to borrow a JRuby instance while holding the write lock!");
        }
        E item = live_queue.takeFirst();
        lock.readLock().lock();
        return item;
    }

    @Override
    public void returnItem(E e) throws InterruptedException {
        live_queue.putFirst(e);
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
