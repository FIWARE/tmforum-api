package org.fiware.tmforum.serviceinventory.domain;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.fiware.serviceinventory.model.ResourceRefVO;
import org.fiware.serviceinventory.model.ServiceRefOrValueVO;
import org.fiware.tmforum.common.domain.*;
import org.fiware.tmforum.resource.Characteristic;
import org.fiware.tmforum.resource.*;
import org.fiware.tmforum.service.*;
import io.github.wistefan.mapping.annotations.AttributeGetter;
import io.github.wistefan.mapping.annotations.AttributeSetter;
import io.github.wistefan.mapping.annotations.AttributeType;
import io.github.wistefan.mapping.annotations.MappingEnabled;
import org.fiware.tmforum.service.ServiceCategoryRef;

import java.net.URI;
import java.time.Instant;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@MappingEnabled(entityType = Service.TYPE_SERVICE_INVENTORY)
public class Service extends EntityWithId {

    public static final String TYPE_SERVICE_INVENTORY = "service-inventory";

    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "href")}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "href")}))
    private URI href;

    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "category")}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "category")}))
    private String category;

    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "description")}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "description")}))
    private String description;

    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "endDate")}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "endDate")}))
    private Instant endDate;

    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "hasStarted")}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "hasStarted")}))
    private Boolean hasStarted;

    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "isBundle")}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "isBundle")}))
    private Boolean isBundle;

    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "isServiceEnabled")}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "isServiceEnabled")}))
    private Boolean isServiceEnabled;

    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "isStateful")}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "isStateful")}))
    private Boolean isStateful;

    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "name")}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "name")}))
    private String name;

    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "serviceDate")}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "serviceDate")}))
    private String serviceDate;

    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "serviceType")}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "serviceType")}))
    private String serviceType;

    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "startDate")}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "startDate")}))
    private Instant startDate;

    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "startMode")}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "startMode")}))
    private String startMode;

    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY_LIST, targetName = "feature")}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY_LIST, targetName = "feature", targetClass = Feature.class)}))
    private List<Feature> feature;

    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY_LIST, targetName = "note")}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY_LIST, targetName = "note", targetClass = Note.class)}))
    private List<Note> note;

    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY_LIST, targetName = "place")}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY_LIST, targetName = "place", targetClass = RelatedPlaceRefOrValue.class)}))
    private List<RelatedPlaceRefOrValue> place;

    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY_LIST, targetName = "relatedEntity")}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY_LIST, targetName = "relatedEntity", targetClass = RelatedEntityRefOrValue.class)}))
    private List<RelatedEntityRefOrValue> relatedEntity;

    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.RELATIONSHIP_LIST, targetName = "relatedParty")}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.RELATIONSHIP_LIST, targetName = "relatedParty", targetClass = RelatedParty.class)}))
    private List<RelatedParty> relatedParty;

    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY_LIST, targetName = "serviceCharacteristic")}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY_LIST, targetName = "serviceCharacteristic", targetClass = Characteristic.class)}))
    private List<Characteristic> serviceCharacteristic;

    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY_LIST, targetName = "serviceOrderItem")}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY_LIST, targetName = "serviceOrderItem", targetClass = RelatedServiceOrderItem.class)}))
    private List<RelatedServiceOrderItem> serviceOrderItem;

    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY_LIST, targetName = "serviceRelationship")}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY_LIST, targetName = "serviceRelationship", targetClass = ServiceRelationship.class)}))
    private List<ServiceRelationship> serviceRelationship;

    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.RELATIONSHIP, targetName = "serviceSpecification")}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.RELATIONSHIP, targetName = "serviceSpecification", targetClass = ServiceSpecificationRef.class)}))
    private ServiceSpecificationRef serviceSpecification;

    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "state")}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "state", targetClass = ServiceStateType.class)}))
    private ServiceStateType state;

    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.RELATIONSHIP_LIST, targetName = "supportingResource")}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.RELATIONSHIP_LIST, targetName = "supportingResource", targetClass = ResourceRef.class)}))
    private List<ResourceRef> supportingResource;

    @Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY_LIST, targetName = "supportingService")}))
    @Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY_LIST, targetName = "supportingService", fromProperties = true, targetClass = ServiceRefOrValue.class)}))
    private List<ServiceRefOrValue> supportingService;

    public Service(String id) {
        super(TYPE_SERVICE_INVENTORY, id);
    }
}
