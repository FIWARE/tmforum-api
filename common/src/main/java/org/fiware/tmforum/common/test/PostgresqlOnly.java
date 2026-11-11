package org.fiware.tmforum.common.test;

import org.junit.jupiter.api.Tag;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A test that pins the postgresql environment and covers nothing a broker job adds: runs only with
 * {@code -Dbroker=postgresql}.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Tag(StorageTags.NOT_ON_ORION_LD)
@Tag(StorageTags.NOT_ON_SCORPIO)
public @interface PostgresqlOnly {
}
