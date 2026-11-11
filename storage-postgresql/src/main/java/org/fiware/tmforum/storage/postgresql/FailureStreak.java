package org.fiware.tmforum.storage.postgresql;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Tracks whether a periodic job is failing, so only the first failure of a streak and the recovery get logged loudly.
 */
final class FailureStreak {

    private final AtomicBoolean failing = new AtomicBoolean();

    /** Records a failure; true only for the first one of a streak. */
    boolean failed() {
        return failing.compareAndSet(false, true);
    }

    /** Records a success; true only when it ends a streak. */
    boolean succeeded() {
        return failing.compareAndSet(true, false);
    }
}
