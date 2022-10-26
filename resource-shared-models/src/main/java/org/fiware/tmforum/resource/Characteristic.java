package org.fiware.tmforum.resource;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.fiware.tmforum.common.domain.EntityWithId;
import org.fiware.tmforum.mapping.annotations.AttributeGetter;
import org.fiware.tmforum.mapping.annotations.AttributeSetter;
import org.fiware.tmforum.mapping.annotations.AttributeType;
import org.fiware.tmforum.mapping.annotations.MappingEnabled;

import java.net.URI;
import java.util.List;

@Data
public class Characteristic {

    private String id;
    private String name;
    private String valueType;
    private Object value;
    private List<CharacteristicRelationship> characteristicRelationship;
    private String atBaseType;
    private URI atSchemaLocation;
    private String atType;
}
