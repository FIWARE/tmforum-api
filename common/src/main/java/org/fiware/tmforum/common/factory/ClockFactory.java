package org.fiware.tmforum.common.factory;

import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Primary;
import jakarta.inject.Singleton;

import java.time.Clock;

@Factory
public class ClockFactory {

    @Primary
    @Singleton
    public Clock clock() {
        return Clock.systemUTC();
    }
}