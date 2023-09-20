package org.fiware.tmforum.usagemanagement.domain;

import java.util.List;
import java.net.URI;

import org.fiware.tmforum.common.domain.RefEntity;
import io.github.wistefan.mapping.annotations.AttributeGetter;
import io.github.wistefan.mapping.annotations.AttributeSetter;
import io.github.wistefan.mapping.annotations.AttributeType;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@EqualsAndHashCode(callSuper = true)
public class UsageSpecificationRef extends RefEntity {

    public UsageSpecificationRef(String id) {
        super(id);
    }

    @Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "href") }))
    @Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "href") }))
    private URI href;

    @Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "name") }))
    @Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "name") }))
    private String name;

    @Override
    public List<String> getReferencedTypes() {
        return List.of(UsageSpecification.TYPE_USP);
    }
    
}
