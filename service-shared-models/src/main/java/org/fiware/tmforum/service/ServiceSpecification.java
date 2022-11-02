package org.fiware.tmforum.servicecatalog.domain;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.fiware.tmforum.common.domain.AttachmentRefOrValue;
import org.fiware.tmforum.common.domain.EntityWithId;
import org.fiware.tmforum.common.domain.RelatedParty;
import org.fiware.tmforum.common.domain.TimePeriod;
import org.fiware.tmforum.mapping.annotations.AttributeGetter;
import org.fiware.tmforum.mapping.annotations.AttributeSetter;
import org.fiware.tmforum.mapping.annotations.AttributeType;
import org.fiware.tmforum.mapping.annotations.MappingEnabled;
import org.fiware.tmforum.resource.ResourceSpecificationRef;
import org.fiware.tmforum.service.CharacteristicSpecification;
import org.fiware.tmforum.service.EntitySpecificationRelationship;
import org.fiware.tmforum.service.FeatureSpecification;
import org.fiware.tmforum.service.ServiceLevelSpecificationRef;
import org.fiware.tmforum.service.ServiceSpecificationRelationship;
import org.fiware.tmforum.service.TargetEntitySchema;

import java.net.URI;
import java.time.Instant;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@MappingEnabled(entityType = ServiceSpecification.TYPE_SERVICE_SPECIFICATION)
public class ServiceSpecification extends EntityWithId {

    public static final String TYPE_SERVICE_SPECIFICATION = "service-specification";

    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "href")}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "href")}))
    private URI href;

    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "description")}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "description")}))
    private String description;

    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "isBundle")}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "isBundle")}))
    private Boolean isBundle;

    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "lastUpdate")}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "lastUpdate")}))
    private Instant lastUpdate;

    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "lifecycleStatus")}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "lifecycleStatus")}))
    private String lifecycleStatus;

    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "name")}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "name")}))
    private String name;

    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "version")}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "version")}))
    private String version;

    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY_LIST, targetName = "attachment")}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY_LIST, targetName = "attachment", targetClass = AttachmentRefOrValue.class)}))
    private List<AttachmentRefOrValue> attachment;

    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.RELATIONSHIP_LIST, targetName = "entitySpecRelationship")}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.RELATIONSHIP_LIST, targetName = "entitySpecRelationship", targetClass = EntitySpecificationRelationship.class)}))
    private List<EntitySpecificationRelationship> entitySpecRelationship;

    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY_LIST, targetName = "featureSpecification")}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY_LIST, targetName = "featureSpecification", targetClass = FeatureSpecification.class)}))
    private List<FeatureSpecification> featureSpecification;

    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.RELATIONSHIP_LIST, targetName = "relatedParty")}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.RELATIONSHIP_LIST, targetName = "relatedParty", targetClass = RelatedParty.class)}))
    private List<RelatedParty> relatedParty;

    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.RELATIONSHIP_LIST, targetName = "resourceSpecification")}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.RELATIONSHIP_LIST, targetName = "resourceSpecification", targetClass = ResourceSpecificationRef.class)}))
    private List<ResourceSpecificationRef> resourceSpecification;

    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.RELATIONSHIP_LIST, targetName = "serviceLevelSpecification")}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.RELATIONSHIP_LIST, targetName = "serviceLevelSpecification", targetClass = ServiceLevelSpecificationRef.class)}))
    private List<ServiceLevelSpecificationRef> serviceLevelSpecification;

    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.RELATIONSHIP_LIST, targetName = "serviceSpecRelationship")}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.RELATIONSHIP_LIST, targetName = "serviceSpecRelationship", targetClass = ServiceSpecificationRelationship.class)}))
    private List<ServiceSpecificationRelationship> serviceSpecRelationship;

    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY_LIST, targetName = "specCharacteristic")}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY_LIST, targetName = "specCharacteristic", targetClass = CharacteristicSpecification.class)}))
    private List<CharacteristicSpecification> specCharacteristic;

    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "targetEntitySchema")}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "targetEntitySchema")}))
    private TargetEntitySchema targetEntitySchema;

    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "validFor")}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "validFor")}))
    private TimePeriod validFor;

    public ServiceSpecification(String id) {
        super(TYPE_SERVICE_SPECIFICATION, id);
    }
}
