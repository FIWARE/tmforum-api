package org.fiware.tmforum.customer;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.runtime.Micronaut;
import org.fiware.tmforum.mapping.EntitiesRepository;
import org.fiware.tmforum.mapping.EntityVOMapper;
import org.fiware.tmforum.mapping.JavaObjectMapper;

/**
 * Base application as starting point
 */
@Factory
public class Application {

    public static void main(String[] args) {
        Micronaut.run(Application.class, args);
    }

    @Bean
    public EntityVOMapper entityVOMapper(ObjectMapper objectMapper, EntitiesRepository entitiesRepository) {
        return new EntityVOMapper(objectMapper, entitiesRepository);
    }

    @Bean
    public JavaObjectMapper javaObjectMapper(ObjectMapper objectMapper) {
        return new JavaObjectMapper();
    }

}