package org.fiware.tmforum.resource;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.fiware.tmforum.common.domain.EntityWithId;
import io.github.wistefan.mapping.annotations.AttributeGetter;
import io.github.wistefan.mapping.annotations.AttributeSetter;
import io.github.wistefan.mapping.annotations.AttributeType;
import io.github.wistefan.mapping.annotations.MappingEnabled;

import java.net.URI;
import java.util.List;

@Data
public class Characteristic {

    private String tmfId;
    private String name;
    private String valueType;
    private Object tmfValue;
    private List<CharacteristicRelationship> characteristicRelationship;
    private String atBaseType;
    private URI atSchemaLocation;
    private String atType;
}
