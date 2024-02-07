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
public class RelatedServiceOrderItem extends Entity {

    private String ItemId;
    private String name;
    private String role;
    private String serviceOrderHref;
    private String serviceOrderId;
    private OrderItemActionType itemAction;
    private String atReferredType;

}