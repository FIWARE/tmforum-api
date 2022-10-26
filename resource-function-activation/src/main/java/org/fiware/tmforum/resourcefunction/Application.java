package org.fiware.tmforum.resourcefunction;


import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.runtime.Micronaut;

import java.time.Clock;

/**
 * Base application as starting point
 */
@Factory
public class Application {

    public static void main(String[] args) {
        Micronaut.run(Application.class, args);
    }

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
