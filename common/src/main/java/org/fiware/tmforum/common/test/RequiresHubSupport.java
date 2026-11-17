package org.fiware.tmforum.common.test;

import org.junit.jupiter.api.Tag;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A hub registration/deletion or event delivery test: skipped on PostgreSQL until ticket 1, since registering a hub
 * still creates an NGSI-LD subscription.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Tag(StorageTags.NOT_ON_POSTGRESQL)
@Tag(StorageTags.TICKET_1)
public @interface RequiresHubSupport {
}
