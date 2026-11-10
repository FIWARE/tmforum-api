package org.fiware.tmforum.storage;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a domain field that reads back absent on every backend until its encoding is decided (see
 * {@link EntityReadRepository#get}). Only the PostgreSQL adapter reads the marker; the NGSI-LD adapter drops today's
 * one field for its own mapping reasons, so a new use needs an NGSI-LD contract case proving it is dropped there too.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface NotStored {
}
