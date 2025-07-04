package org.fiware.tmforum.usagemanagement.domain;

import java.time.Instant;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.fiware.tmforum.common.domain.Entity;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class UsageCharacteristic extends Entity {
    private String id;
    private String name;
    private String valueType;
    private List<CharacteristicRelationship> characteristicRelationship;
    private Object charValue;
}