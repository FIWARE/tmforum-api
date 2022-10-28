package org.fiware.tmforum.mapping.desc.pojos.invalid;

import org.fiware.tmforum.mapping.annotations.MappingEnabled;

@MappingEnabled(entityType = "throwing-pojo")
public class MyThrowingConstructor {

    public MyThrowingConstructor(String id) {
        throw new RuntimeException("Something is really wrong.");
    }
}
