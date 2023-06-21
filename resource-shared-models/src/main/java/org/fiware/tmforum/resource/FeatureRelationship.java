package org.fiware.tmforum.resource;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.fiware.tmforum.common.domain.TimePeriod;
import io.github.wistefan.mapping.annotations.AttributeGetter;
import io.github.wistefan.mapping.annotations.AttributeSetter;
import io.github.wistefan.mapping.annotations.AttributeType;
import io.github.wistefan.mapping.annotations.DatasetId;
import io.github.wistefan.mapping.annotations.RelationshipObject;

import java.net.URI;
import java.util.List;

@Data
public class FeatureRelationship {

    private String id;
    private String name;
    private TimePeriod validFor;
    private String relationshipType;
}
