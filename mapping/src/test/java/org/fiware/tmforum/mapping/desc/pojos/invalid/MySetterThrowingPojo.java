package org.fiware.tmforum.mapping.desc.pojos.invalid;

import lombok.Builder;
import org.fiware.tmforum.mapping.annotations.AttributeGetter;
import org.fiware.tmforum.mapping.annotations.AttributeSetter;
import org.fiware.tmforum.mapping.annotations.AttributeType;
import org.fiware.tmforum.mapping.annotations.EntityId;
import org.fiware.tmforum.mapping.annotations.EntityType;
import org.fiware.tmforum.mapping.annotations.MappingEnabled;
import org.fiware.tmforum.mapping.desc.pojos.MySubPropertyEntity;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Builder
@MappingEnabled(entityType = "my-pojo")
public class MySetterThrowingPojo {

    public Consumer<String> attributeConsumer;

    @AttributeSetter(value = AttributeType.PROPERTY, targetName = "name")
    public void setName(String name) {
        throw new RuntimeException("Something is wrong with me.");
    }

}
