package org.fiware.tmforum.productordering;

import jakarta.inject.Singleton;

/**
 * Allows to run a setup function only once in a multi-thread testing scenario
 */

@Singleton
public class TestSetupTracker {
    private boolean setupDone = false;

    public synchronized boolean isSetupDone() {
        return setupDone;
    }

    public synchronized void markSetupDone() {
        this.setupDone = true;
    }

    public synchronized void reset() {
        this.setupDone = false;
    }
}
