package org.fiware.tmforum.resourcefunction.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.fiware.tmforum.common.domain.RefEntity;
import io.github.wistefan.mapping.annotations.AttributeGetter;
import io.github.wistefan.mapping.annotations.AttributeSetter;
import io.github.wistefan.mapping.annotations.AttributeType;

import java.util.ArrayList;
import java.util.List;

@EqualsAndHashCode
public class EndpointRef extends RefEntity {

    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "isRoot")}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "isRoot")}))
    private Boolean isRoot;


    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.RELATIONSHIP, targetName = "connectionPoint")}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.RELATIONSHIP, targetName = "connectionPoint", targetClass = ConnectionPointRef.class)}))
    private ConnectionPointRef connectionPoint;

    public EndpointRef(@JsonProperty("id") String id) {
        super(id);
    }

    @Override
    @JsonIgnore
    public List<String> getReferencedTypes() {
        return new ArrayList<>(List.of(getAtReferredType()));
    }
}
