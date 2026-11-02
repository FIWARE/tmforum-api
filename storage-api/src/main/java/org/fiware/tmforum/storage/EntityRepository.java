package org.fiware.tmforum.storage;

/**
 * Full entity port, as injected by controllers that read and write.
 */
public interface EntityRepository extends EntityReadRepository, EntityWriteRepository {
}
