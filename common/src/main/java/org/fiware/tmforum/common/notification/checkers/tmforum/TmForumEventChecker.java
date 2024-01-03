package org.fiware.tmforum.common.notification.checkers.tmforum;

public interface TmForumEventChecker {
    boolean wasFired(String query);
}
