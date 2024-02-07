package org.fiware.tmforum.serviceinventory.domain;

import io.github.wistefan.mapping.annotations.AttributeGetter;
import io.github.wistefan.mapping.annotations.AttributeSetter;
import io.github.wistefan.mapping.annotations.AttributeType;
import lombok.EqualsAndHashCode;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.fiware.tmforum.common.domain.Entity;
import org.fiware.tmforum.common.domain.*;
import org.fiware.tmforum.resource.Characteristic;
import org.fiware.tmforum.common.validation.ReferencedEntity;
import org.fiware.tmforum.resource.*;
import org.fiware.tmforum.service.*;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.time.Instant;

@EqualsAndHashCode(callSuper = true)
@Data
public class ServiceRefOrValue extends Entity implements ReferencedEntity {

    private URI id;
    private URI href;
    private String category;
    private String description;
    private Instant endDate;
    private Boolean hasStarted;
    private Boolean isBundle;
    private Boolean isServiceEnabled;
    private Boolean isStateful;
    private String name;
    private String serviceDate;
    private String serviceType;
    private String startDate;
    private String startMode;
    private List<Feature> feature;
    private List<Note> note;
    private List<RelatedPlaceRefOrValue> place;
    private List<RelatedEntityRefOrValue> relatedEntity;
    private List<RelatedParty> relatedParty;
    private List<Characteristic> serviceCharacteristic;
    private List<RelatedServiceOrderItem> serviceOrderItem;
    private List<ServiceRelationship> serviceRelationship;
    private ServiceSpecificationRef serviceSpecification;
    private ServiceStateType state;
    private List<ResourceRef> supportingResource;
    private List<ServiceRefOrValue> supportingService;
    private String atBaseType;
    private URI atSchemaLocation;
    private String atType;
    private String atReferredType;

    @Override
    public List<String> getReferencedTypes() {
        return new ArrayList<>(List.of("service"));
    }

    @Override
    public URI getEntityId() {
        return this.id;
    }

}
