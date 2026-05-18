package org.fiware.tmforum.common.querying;

import io.github.wistefan.mapping.annotations.AttributeGetter;
import io.github.wistefan.mapping.annotations.AttributeSetter;
import io.github.wistefan.mapping.annotations.AttributeType;
import io.github.wistefan.mapping.annotations.EntityId;
import io.github.wistefan.mapping.annotations.EntityType;
import io.github.wistefan.mapping.annotations.MappingEnabled;
import org.fiware.tmforum.common.domain.Entity;

import java.net.URI;

/**
 * Minimal {@link Entity} subclass used by {@link QueryParserTest} to verify
 * the JSON-LD reserved-token translation. Mirrors the {@link MyPojo} fixture
 * but extends {@code Entity}, which contributes the {@code atType /
 * atBaseType / atSchemaLocation} attributes via the inherited wistefan
 * annotations.
 */
@MappingEnabled(entityType = "my-entity-pojo")
public class MyEntityPojo extends Entity {

    private static final String ENTITY_TYPE = "my-entity-pojo";

    private final URI id;
    private String status;

    public MyEntityPojo(String id) {
        this.id = URI.create(id);
    }

    @EntityId
    public URI getId() {
        return id;
    }

    @EntityType
    public String getType() {
        return ENTITY_TYPE;
    }

    @AttributeGetter(value = AttributeType.PROPERTY, targetName = "status")
    public String getStatus() {
        return status;
    }

    @AttributeSetter(value = AttributeType.PROPERTY, targetName = "status")
    public void setStatus(String status) {
        this.status = status;
    }
}
