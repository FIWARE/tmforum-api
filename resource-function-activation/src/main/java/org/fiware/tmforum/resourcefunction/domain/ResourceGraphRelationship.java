package org.fiware.tmforum.resourcefunction.domain;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.fiware.tmforum.common.domain.Entity;
import org.fiware.tmforum.common.domain.RefEntity;
import org.fiware.tmforum.common.validation.ReferencedEntity;
import org.fiware.tmforum.mapping.annotations.AttributeGetter;
import org.fiware.tmforum.mapping.annotations.AttributeSetter;
import org.fiware.tmforum.mapping.annotations.AttributeType;
import org.fiware.tmforum.mapping.annotations.DatasetId;
import org.fiware.tmforum.mapping.annotations.MappingEnabled;
import org.fiware.tmforum.mapping.annotations.RelationshipObject;

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
