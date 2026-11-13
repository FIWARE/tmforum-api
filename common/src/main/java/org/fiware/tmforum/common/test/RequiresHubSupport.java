package org.fiware.tmforum.common.test;

import org.junit.jupiter.api.Tag;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A hub registration/deletion or event delivery test: skipped on PostgreSQL until both ticket 1 (no NGSI-LD
 * subscription for a hub) and ticket 7 (the hub query and the subscription lookup) have landed.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Tag(StorageTags.NOT_ON_POSTGRESQL)
@Tag(StorageTags.TICKET_1)
@Tag(StorageTags.TICKET_7)
public @interface RequiresHubSupport {
}
