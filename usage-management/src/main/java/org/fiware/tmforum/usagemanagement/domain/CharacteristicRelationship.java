package org.fiware.tmforum.usagemanagement.domain;

import java.time.Instant;
import java.util.List;
import java.net.URI;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.fiware.tmforum.common.domain.Entity;

@Data
@EqualsAndHashCode(callSuper = true)
public class CharacteristicRelationship extends Entity {
    private String tmfId;
    private URI href;
    private String relationshipType;
}