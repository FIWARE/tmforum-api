package org.fiware.tmforum.party.domain.organization;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import io.github.wistefan.mapping.annotations.AttributeGetter;
import io.github.wistefan.mapping.annotations.AttributeSetter;
import io.github.wistefan.mapping.annotations.AttributeType;
import org.fiware.tmforum.common.domain.RefEntity;

import java.util.ArrayList;
import java.util.List;

@EqualsAndHashCode
public abstract class OrganizationRelationship extends RefEntity {

    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "relationshipType", embedProperty = true)}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "relationshipType", targetClass = String.class)}))
    private String relationshipType;

    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "name", embedProperty = true)}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "name", targetClass = String.class)}))
    private String name;

    protected OrganizationRelationship(String id) {
        super(id);
    }

    @Override
    @JsonIgnore
    public List<String> getReferencedTypes() {
        return new ArrayList<>(List.of(Organization.TYPE_ORGANIZATION));
    }
}


