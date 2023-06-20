package org.fiware.tmforum.resourcefunction.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.fiware.tmforum.common.domain.EntityWithId;
import org.fiware.tmforum.common.validation.ReferencedEntity;
import io.github.wistefan.mapping.annotations.AttributeGetter;
import io.github.wistefan.mapping.annotations.AttributeSetter;
import io.github.wistefan.mapping.annotations.AttributeType;
import io.github.wistefan.mapping.annotations.MappingEnabled;

import java.net.URI;
import java.util.List;

@Data
public class ResourceGraph {

    private String id;
    private String description;
    private String name;
    private List<Connection> connection;
    private List<ResourceGraphRelationship> graphRelationship;
    private String atBaseType;
    private String atSchemaLocation;
    private String atType;
}
