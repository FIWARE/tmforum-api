package org.fiware.tmforum.common.test;

import org.junit.jupiter.api.Tag;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A list test with a filter or sorting: skipped on PostgreSQL until ticket 7 translates queries for it.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Tag(StorageTags.NOT_ON_POSTGRESQL)
@Tag(StorageTags.TICKET_7)
public @interface RequiresQueryTranslation {
}
