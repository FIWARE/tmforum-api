package org.fiware.tmforum.common.test;

import org.junit.jupiter.api.Tag;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A test of the NGSI-LD broker itself (its adapter, subscriptions or representation): never runs on PostgreSQL.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Tag(StorageTags.NOT_ON_POSTGRESQL)
@Tag(StorageTags.BROKER_ONLY)
public @interface BrokerOnly {
}
