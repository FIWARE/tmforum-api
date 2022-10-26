package org.fiware.tmforum.resourcecatalog.domain;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.fiware.tmforum.common.validation.ReferencedEntity;
import org.fiware.tmforum.mapping.annotations.DatasetId;
import org.fiware.tmforum.mapping.annotations.RelationshipObject;

import java.net.URI;
import java.util.List;

@EqualsAndHashCode
public class ResourceSpecificationRef implements ReferencedEntity {

    @Getter(onMethod = @__({@RelationshipObject, @DatasetId}))
    private URI id;

    public ResourceSpecificationRef(URI id) {
        this.id = id;
    }

    @Override
    public List<String> getReferencedTypes() {
        return List.of(ResourceSpecification.TYPE_RESOURCE_SPECIFICATION);
    }
}
