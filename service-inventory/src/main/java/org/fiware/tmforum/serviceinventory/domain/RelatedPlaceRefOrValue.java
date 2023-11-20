package org.fiware.tmforum.serviceinventory.domain;

import io.github.wistefan.mapping.annotations.AttributeGetter;
import io.github.wistefan.mapping.annotations.AttributeSetter;
import io.github.wistefan.mapping.annotations.AttributeType;
import lombok.EqualsAndHashCode;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.fiware.tmforum.common.domain.Entity;

import java.net.URI;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class RelatedPlaceRefOrValue extends Entity {

    private URI id;
    private URI href;
    private String name;
    private String role;
    private String atBaseType;
    private URI atSchemaLocation;
    private String atType;
    private String atReferredType;

}
