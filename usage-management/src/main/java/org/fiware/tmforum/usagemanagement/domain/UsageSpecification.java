package org.fiware.tmforum.usagemanagement.domain;

import java.net.URI;
import java.time.Instant;
import java.util.List;

import org.fiware.tmforum.common.domain.EntityWithId;
import org.fiware.tmforum.common.domain.RelatedParty;
import org.fiware.tmforum.common.domain.ConstraintRef;
import org.fiware.tmforum.common.domain.AttachmentRefOrValue;
import org.fiware.tmforum.common.domain.TimePeriod;
import org.fiware.tmforum.service.CharacteristicSpecification;
import org.fiware.tmforum.service.EntitySpecificationRelationship;
import org.fiware.tmforum.service.TargetEntitySchema;

import io.github.wistefan.mapping.annotations.AttributeGetter;
import io.github.wistefan.mapping.annotations.AttributeSetter;
import io.github.wistefan.mapping.annotations.AttributeType;
import io.github.wistefan.mapping.annotations.MappingEnabled;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@EqualsAndHashCode(callSuper = true)
@MappingEnabled(entityType = UsageSpecification.TYPE_USP)
public class UsageSpecification extends EntityWithId {
    public static final String TYPE_USP = "usageSpecification";

    public UsageSpecification(String id) {
		super(TYPE_USP, id);
	}

	@Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "href") }))
	@Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "href") }))
	private URI href;

    @Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "description") }))
	@Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "description") }))
	private String description;

    @Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "isBundle") }))
	@Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "isBundle") }))
	private Boolean isBundle; //Era bool e cambieino a String

    @Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "lastUpdate") }))
	@Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "lastUpdate") }))
	private Instant lastUpdate;

    @Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "lifecycleStatus") }))
	@Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "lifecycleStatus") }))
	private String lifecycleStatus;

    @Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "name") }))
	@Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "name") }))
	private String name;

	@Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "version") }))
	@Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "version") }))
	private String version;

    @Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY_LIST, targetName = "attachment") }))
	@Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY_LIST, targetName = "attachment") }))
	private List<AttachmentRefOrValue> attachment;

    @Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.RELATIONSHIP_LIST, targetName = "constraint") }))
	@Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.RELATIONSHIP_LIST, targetName = "constraint", fromProperties = true) }))
	private List<ConstraintRef> constraint; 

    @Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.RELATIONSHIP_LIST, targetName = "entitySpecRelationship") }))
	@Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.RELATIONSHIP_LIST, targetName = "entitySpecRelationship", fromProperties = true) }))
	private List<EntitySpecificationRelationship> entitySpecRelationship;

    @Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.RELATIONSHIP_LIST, targetName = "relatedParty") }))
	@Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.RELATIONSHIP_LIST, targetName = "relatedParty", fromProperties = true) }))
	private List<RelatedParty> relatedParty;

    @Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY_LIST, targetName = "specCharacteristic") }))
	@Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY_LIST, targetName = "specCharacteristic") }))
	private List<CharacteristicSpecification> specCharacteristic;

    @Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "targetEntitySchema") }))
	@Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "targetEntitySchema") }))
	private TargetEntitySchema targetEntitySchema;

    @Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "validFor") }))
	@Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "validFor") }))
	private TimePeriod validFor;
}