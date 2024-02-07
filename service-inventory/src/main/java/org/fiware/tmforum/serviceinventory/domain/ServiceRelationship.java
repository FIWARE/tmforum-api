package org.fiware.tmforum.serviceinventory.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.fiware.tmforum.resource.*;
import org.fiware.tmforum.common.domain.Entity;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class ServiceRelationship extends Entity {

    private String relationshipType;
    private ServiceRefOrValue service;
    private List<Characteristic> serviceRelationshipCharacteristic;
}
