package org.fiware.tmforum.resourcecatalog.domain;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.fiware.tmforum.common.domain.Entity;
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

@EqualsAndHashCode(callSuper = true)
@MappingEnabled(entityType = Resource.TYPE_RESOURCE)
public class ResourceRelationship extends Entity implements ReferencedEntity {

    @Getter(onMethod = @__({@RelationshipObject, @DatasetId}))
    @Nullable
    URI id;

    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "relationshipType", embedProperty = true)}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "relationshipType", fromProperties = true)}))
    private String relationshipType;

    // this is not annotated with attribut getter/setter specific information, since it should be ignored by our mappers and will be explicitly handled due to the
    // ref or value mechanism
    @Getter
    @Setter
    private Resource resource;

    public ResourceRelationship(URI id) {
        this.id = id;
    }

    @Override
    public List<String> getReferencedTypes() {
        return List.of(Resource.TYPE_RESOURCE);
    }
}
