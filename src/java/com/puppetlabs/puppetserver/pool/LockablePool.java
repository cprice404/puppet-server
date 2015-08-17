package com.puppetlabs.puppetserver.pool;

public interface LockablePool<E> {
    public void register(E e) throws InterruptedException;

    public E borrowItem() throws InterruptedException;

    public void returnItem(E e) throws InterruptedException;

    public void lock() throws InterruptedException;

    public void unlock() throws InterruptedException;
}
