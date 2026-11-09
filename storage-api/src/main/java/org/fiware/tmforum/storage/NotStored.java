package org.fiware.tmforum.storage;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a domain field no adapter persists until its encoding is decided, so it reads back absent on every backend
 * (see {@link EntityReadRepository#get}).
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface NotStored {
}
