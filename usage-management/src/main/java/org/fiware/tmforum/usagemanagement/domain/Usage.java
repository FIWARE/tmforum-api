package org.fiware.tmforum.usagemanagement.domain;

import java.net.URI;
import java.time.Instant;
import java.util.List;

import org.fiware.tmforum.common.domain.EntityWithId;
import org.fiware.tmforum.common.domain.RelatedParty;

import io.github.wistefan.mapping.annotations.AttributeGetter;
import io.github.wistefan.mapping.annotations.AttributeSetter;
import io.github.wistefan.mapping.annotations.AttributeType;
import io.github.wistefan.mapping.annotations.MappingEnabled;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;


@EqualsAndHashCode(callSuper = true)
@MappingEnabled(entityType = Usage.TYPE_U)
public class Usage extends EntityWithId {
	public static final String TYPE_U = "usage";

    public Usage(String id) {
		super(TYPE_U, id);
	}

	@Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "href") }))
	@Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "href") }))
	private URI href;

    @Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "description") }))
	@Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "description") }))
	private String description;

    @Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "usageDate") }))
	@Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "usageDate") }))
	private Instant usageDate;

    @Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "usageType") }))
	@Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "usageType") }))
	private String usageType;

    @Getter(onMethod = @__({
        @AttributeGetter(value = AttributeType.PROPERTY_LIST, targetName = "ratedProductUsage") }))
    @Setter(onMethod = @__({
        @AttributeSetter(value = AttributeType.PROPERTY_LIST, targetName = "ratedProductUsage", targetClass = RatedProductUsage.class) }))
    private List<RatedProductUsage> ratedProductUsage;

    @Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.RELATIONSHIP_LIST, targetName = "relatedParty") }))
	@Setter(onMethod = @__({
			@AttributeSetter(value = AttributeType.RELATIONSHIP_LIST, targetName = "relatedParty", targetClass = RelatedParty.class, fromProperties = true) }))
	private List<RelatedParty> relatedParty;

    @Getter(onMethod = @__({
        @AttributeGetter(value = AttributeType.PROPERTY_LIST, targetName = "status") }))
    @Setter(onMethod = @__({
        @AttributeSetter(value = AttributeType.PROPERTY_LIST, targetName = "status", targetClass = UsageStatusType.class) }))
    private UsageStatusType usageStatusType;

    @Getter(onMethod = @__({
        @AttributeGetter(value = AttributeType.PROPERTY_LIST, targetName = "usageCharacteristic") }))
    @Setter(onMethod = @__({
        @AttributeSetter(value = AttributeType.PROPERTY_LIST, targetName = "usageCharacteristic", targetClass = UsageCharacteristic.class) }))
    private List<UsageCharacteristic> usageCharacteristic;

    @Getter(onMethod = @__({
        @AttributeGetter(value = AttributeType.RELATIONSHIP, targetName = "usageSpecification") }))
    @Setter(onMethod = @__({
        @AttributeSetter(value = AttributeType.RELATIONSHIP, targetName = "usageSpecification", targetClass = UsageSpecificationRef.class) }))
    private UsageSpecificationRef usageSpecification;

}
