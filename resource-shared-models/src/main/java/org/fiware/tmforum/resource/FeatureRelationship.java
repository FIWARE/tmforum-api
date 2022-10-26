package org.fiware.tmforum.resource;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.fiware.tmforum.common.domain.TimePeriod;
import org.fiware.tmforum.mapping.annotations.AttributeGetter;
import org.fiware.tmforum.mapping.annotations.AttributeSetter;
import org.fiware.tmforum.mapping.annotations.AttributeType;
import org.fiware.tmforum.mapping.annotations.DatasetId;
import org.fiware.tmforum.mapping.annotations.RelationshipObject;

import java.net.URI;
import java.util.List;

@Data
public class FeatureRelationship {

    private String id;
    private String name;
    private TimePeriod validFor;
    private String relationshipType;
}
