package org.fiware.tmforum;

import io.micronaut.context.annotation.Factory;
import io.micronaut.runtime.Micronaut;

/**
 * All-in-one application that combines all TMForum API modules into a single server.
 */
@Factory
public class Application {

    public static void main(String[] args) {
        Micronaut.build(args)
                .packages("org.fiware")
                .mainClass(Application.class)
                .start();
    }
}
