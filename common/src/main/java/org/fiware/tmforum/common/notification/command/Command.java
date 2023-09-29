package org.fiware.tmforum.common.notification.command;

@FunctionalInterface
public interface Command {
    boolean execute(String query);
}
