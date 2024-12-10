package org.fiware.tmforum.common.domain;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.github.wistefan.mapping.annotations.AttributeGetter;
import io.github.wistefan.mapping.annotations.AttributeSetter;
import io.github.wistefan.mapping.annotations.AttributeType;
import io.github.wistefan.mapping.annotations.DatasetId;
import io.github.wistefan.mapping.annotations.EntityId;
import io.github.wistefan.mapping.annotations.EntityType;
import io.github.wistefan.mapping.annotations.Ignore;
import io.github.wistefan.mapping.annotations.RelationshipObject;
import lombok.Getter;
import lombok.Setter;
import org.fiware.ngsi.model.AdditionalPropertyVO;
import org.fiware.tmforum.common.mapping.IdHelper;

import javax.annotation.Nullable;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * Abstract superclass for all entities with an id
 */
public abstract class EntityWithId extends Entity {

    /**
     * Type of the entity
     */
    @Getter(onMethod = @__({@EntityType}))
    final String type;

    /**
     * Id of the entity. This is the id part of "urn:ngsi-ld:TYPE:ID"
     */
    @Ignore
    @Getter(onMethod = @__({@EntityId, @RelationshipObject, @DatasetId}))
    @Setter
    URI id;

    protected EntityWithId(String type, String id) {
        this.type = type;
        if (IdHelper.isNgsiLdId(id)) {
            this.id = URI.create(id);
        } else {
            this.id = IdHelper.toNgsiLd(id, type);
        }
    }

}
