package org.fiware.tmforum.productcatalog;


import io.micronaut.context.annotation.Factory;
import io.micronaut.runtime.Micronaut;

/**
 * Base application as starting point
 */
@Factory
public class Application {

    public static void main(String[] args) {
        Micronaut.run(Application.class, args);
    }
}
