package org.fiware.tmforum.resource;

import lombok.Data;
import org.fiware.tmforum.common.domain.TimePeriod;

import java.net.URI;

@Data
public class FeatureRelationship {

    private String id;
    private URI href;
    private String name;
    private TimePeriod validFor;
    private String relationshipType;
}
