package org.fiware.tmforum.service;

import lombok.Data;
import org.fiware.tmforum.common.domain.ConstraintRef;
import org.fiware.tmforum.common.domain.TimePeriod;

import java.net.URI;
import java.util.List;

@Data
public class FeatureSpecification {

	private String tmfId;
	private Boolean isBundle;
	private Boolean isEnabled;
	private String name;
	private String version;
	private List<ConstraintRef> constraint;
	private List<FeatureSpecificationCharacteristic> featureSpecCharacteristic;
	private List<FeatureSpecificationRelationship> featureSpecRelationship;
	private TimePeriod validFor;
	private String atBaseType;
	private URI atSchemaLocation;
	private String atType;
}
