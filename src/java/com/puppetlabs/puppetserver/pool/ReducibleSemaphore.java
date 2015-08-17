package com.puppetlabs.puppetserver.pool;

import java.util.concurrent.Semaphore;

/**
 * This class extends Semaphore, for the sole purpose of granting
 * public access to its protected "reducePermits" method, which
 * turns out to be incredibly useful.
 */
public class ReducibleSemaphore extends Semaphore {
    public ReducibleSemaphore(int permits) {
        super(permits);
    }
    public void reduce(int reduction) {
        reducePermits(reduction);
    }
}
