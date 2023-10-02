package org.fiware.tmforum.partyrole.domain;

import java.net.URI;
import java.util.List;

import org.fiware.tmforum.common.domain.RefEntity;

import io.github.wistefan.mapping.annotations.AttributeGetter;
import io.github.wistefan.mapping.annotations.AttributeSetter;
import io.github.wistefan.mapping.annotations.AttributeType;
import io.github.wistefan.mapping.annotations.Ignore;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@EqualsAndHashCode(callSuper = true)
public class AccountRef extends RefEntity {
    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "description", embedProperty = true)}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "description", targetClass = String.class)}))
    private String description;

    public AccountRef(URI id) {
        super(id);
    }

    // TODO: fix when account domain is created
    @Override
    @Ignore
    public List<String> getReferencedTypes() {
        return List.of("account");
    }
}
