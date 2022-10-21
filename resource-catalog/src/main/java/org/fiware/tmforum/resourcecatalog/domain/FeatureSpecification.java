package org.fiware.tmforum.resourcecatalog.domain;

import lombok.Data;
import org.fiware.tmforum.common.domain.ConstraintRef;

import java.util.List;

@Data
public class FeatureSpecification {

    private Boolean isBundle;
    private Boolean isEnabled;
    private String name;
    private String version;
    // validate ref
    private List<ConstraintRef> constraint;
    private List<FeatureSpecificationCharacteristic> featureSpecCharacteristic;
    private List<FeatureSpecificationRelationship> featureSpecRelationship;
}
