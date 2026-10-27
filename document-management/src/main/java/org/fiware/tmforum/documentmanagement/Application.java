package org.fiware.tmforum.documentmanagement;

import io.micronaut.context.annotation.Factory;
import io.micronaut.runtime.Micronaut;
import jakarta.inject.Singleton;
import org.fiware.document.model.DocumentSpecificationVO;
import org.fiware.tmforum.common.mapping.FieldCleaningModule;

import java.util.List;

@Factory
public class Application {

    public static void main(String[] args) {
        Micronaut.run(Application.class, args);
    }

    @Singleton
    static class VOFieldCleaningModule extends FieldCleaningModule {

        VOFieldCleaningModule() {
            super(List.of(DocumentSpecificationVO.class));
        }
    }
}
