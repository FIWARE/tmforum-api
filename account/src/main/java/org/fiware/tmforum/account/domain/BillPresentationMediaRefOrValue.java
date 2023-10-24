package org.fiware.tmforum.account.domain;

import io.github.wistefan.mapping.annotations.AttributeGetter;
import io.github.wistefan.mapping.annotations.AttributeSetter;
import io.github.wistefan.mapping.annotations.AttributeType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.fiware.tmforum.common.domain.Entity;

import java.net.URI;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class BillPresentationMediaRefOrValue extends Entity {

    private URI id;
    private URI href;
    private String description;
    private Boolean isRef;
    private String name;
    private String atBaseType;
    private URI atSchemaLocation;
    private String atReferredType;
}
