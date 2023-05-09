package org.fiware.tmforum.resourcefunction.domain;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.fiware.tmforum.common.domain.Entity;
import org.fiware.tmforum.common.domain.RefEntity;
import org.fiware.tmforum.common.validation.ReferencedEntity;
import io.github.wistefan.mapping.annotations.AttributeGetter;
import io.github.wistefan.mapping.annotations.AttributeSetter;
import io.github.wistefan.mapping.annotations.AttributeType;
import io.github.wistefan.mapping.annotations.DatasetId;
import io.github.wistefan.mapping.annotations.MappingEnabled;
import io.github.wistefan.mapping.annotations.RelationshipObject;

import javax.annotation.Nullable;
import java.net.URI;
import java.util.List;

@Data
public class ResourceGraphRelationship {

    private String relationshipType;
    private ResourceGraphRef resourceGraph;
    private String atBaseType;
    private String atSchemaLocation;
    private String atType;
}
