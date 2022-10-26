package org.fiware.tmforum.resourcefunction.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.fiware.tmforum.common.domain.EntityWithId;
import org.fiware.tmforum.common.validation.ReferencedEntity;
import org.fiware.tmforum.mapping.annotations.AttributeGetter;
import org.fiware.tmforum.mapping.annotations.AttributeSetter;
import org.fiware.tmforum.mapping.annotations.AttributeType;
import org.fiware.tmforum.mapping.annotations.MappingEnabled;

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
