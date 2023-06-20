package org.fiware.tmforum.resource;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import io.github.wistefan.mapping.annotations.AttributeGetter;
import io.github.wistefan.mapping.annotations.AttributeSetter;
import io.github.wistefan.mapping.annotations.AttributeType;

import java.net.URI;
import java.util.List;

@Data
public class CharacteristicRelationship {

    private String id;
    private URI href;
    private String relationshipType;
    private String atBaseType;
    private String atSchemaLocation;
    private String atType;
}
