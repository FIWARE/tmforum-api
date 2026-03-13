package org.fiware.tmforum.common.notification;

/**
 * Marker interface for module-specific EventMapper implementations.
 * Allows distinguishing individual module mappers from composite ones,
 * enabling all-in-one deployments to aggregate them without conflicts.
 */
public interface ModuleEventMapper extends EventMapper {
}
