package org.fiware.tmforum.resource;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.fiware.tmforum.common.domain.ConstraintRef;
import org.fiware.tmforum.common.domain.EntityWithId;
import org.fiware.tmforum.mapping.annotations.AttributeGetter;
import org.fiware.tmforum.mapping.annotations.AttributeSetter;
import org.fiware.tmforum.mapping.annotations.AttributeType;
import org.fiware.tmforum.mapping.annotations.MappingEnabled;

import java.net.URI;
import java.util.List;

@Data
public class Feature {

    private String id;
    private Boolean isBundle;
    private Boolean isEnabled;
    private String name;
    private List<ConstraintRef> constraint;
    private List<Characteristic> featureCharacteristic;
    private List<FeatureRelationship> featureRelationship;
    private String atBaseType;
    private URI atSchemaLocation;
    private String atType;

}
