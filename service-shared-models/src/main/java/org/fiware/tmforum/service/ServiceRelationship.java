package org.fiware.tmforum.service;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.fiware.tmforum.common.domain.Entity;

@Data
@EqualsAndHashCode(callSuper = true)
public class ServiceRelationship extends Entity {

    private String relationshipType;
    // needs to work as ref or value!
    private ServiceRef service;
}