package org.fiware.tmforum.storage;

import jakarta.inject.Qualifier;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks the bean that actually talks to a backend; every adapter carries it and is chosen by {@code @Requires} alone.
 * A decorator injects the port with this qualifier, never names an adapter class and never carries it itself.
 */
@Qualifier
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER})
public @interface StorageAdapter {
}
