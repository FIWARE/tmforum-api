package org.fiware.tmforum.product;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.fiware.tmforum.common.domain.AgreementRef;
import org.fiware.tmforum.common.domain.EntityWithId;
import org.fiware.tmforum.common.domain.PlaceRef;
import io.github.wistefan.mapping.annotations.AttributeGetter;
import io.github.wistefan.mapping.annotations.AttributeSetter;
import io.github.wistefan.mapping.annotations.AttributeType;
import io.github.wistefan.mapping.annotations.MappingEnabled;
import org.fiware.tmforum.common.domain.AttachmentRefOrValue;
import org.fiware.tmforum.common.domain.TimePeriod;
import org.fiware.tmforum.resource.ResourceCandidateRef;
import org.fiware.tmforum.service.ServiceCandidateRef;

import java.net.URI;
import java.time.Instant;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@MappingEnabled(entityType = ProductOffering.TYPE_PRODUCT_OFFERING)
public class ProductOffering extends EntityWithId {

	public static final String TYPE_PRODUCT_OFFERING = "product-offering";

	@Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "href")}))
	@Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "href")}))
	private URI href;

	@Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "description")}))
	@Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "description")}))
	private String description;

	@Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "isBundle")}))
	@Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "isBundle")}))
	private Boolean isBundle;

	@Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "isSellable")}))
	@Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "isSellable")}))
	private Boolean isSellable;

	@Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "lastUpdate")}))
	@Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "lastUpdate")}))
	private Instant lastUpdate;

	@Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "lifecycleStatus")}))
	@Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "lifecycleStatus")}))
	private String lifecycleStatus;

	@Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "name")}))
	@Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "name")}))
	private String name;

	@Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "statusReason")}))
	@Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "statusReason")}))
	private String statusReason;

	@Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "version")}))
	@Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "version")}))
	private String version;

	@Getter(onMethod = @__({@AttributeGetter(value = AttributeType.RELATIONSHIP_LIST, targetName = "agreement")}))
	@Setter(onMethod = @__({@AttributeSetter(value = AttributeType.RELATIONSHIP_LIST, targetName = "agreement", targetClass = AgreementRef.class)}))
	private List<AgreementRef> agreement;

	@Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY_LIST, targetName = "attachment")}))
	@Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY_LIST, targetName = "attachment", targetClass = AttachmentRefOrValue.class)}))
	private List<AttachmentRefOrValue> attachment;

	@Getter(onMethod = @__({@AttributeGetter(value = AttributeType.RELATIONSHIP_LIST, targetName = "bundledProductOffering")}))
	@Setter(onMethod = @__({@AttributeSetter(value = AttributeType.RELATIONSHIP_LIST, targetName = "bundledProductOffering", targetClass = BundleProductOffering.class)}))
	private List<BundleProductOffering> bundledProductOffering;

	@Getter(onMethod = @__({@AttributeGetter(value = AttributeType.RELATIONSHIP_LIST, targetName = "category")}))
	@Setter(onMethod = @__({@AttributeSetter(value = AttributeType.RELATIONSHIP_LIST, targetName = "category", targetClass = CategoryRef.class)}))
	private List<CategoryRef> category;

	@Getter(onMethod = @__({@AttributeGetter(value = AttributeType.RELATIONSHIP_LIST, targetName = "channel")}))
	@Setter(onMethod = @__({@AttributeSetter(value = AttributeType.RELATIONSHIP_LIST, targetName = "channel", targetClass = ChannelRef.class)}))
	private List<ChannelRef> channel;

	@Getter(onMethod = @__({@AttributeGetter(value = AttributeType.RELATIONSHIP_LIST, targetName = "marketSegment")}))
	@Setter(onMethod = @__({@AttributeSetter(value = AttributeType.RELATIONSHIP_LIST, targetName = "marketSegment", targetClass = MarketSegmentRef.class)}))
	private List<MarketSegmentRef> marketSegment;

	@Getter(onMethod = @__({@AttributeGetter(value = AttributeType.RELATIONSHIP_LIST, targetName = "place")}))
	@Setter(onMethod = @__({@AttributeSetter(value = AttributeType.RELATIONSHIP_LIST, targetName = "place", targetClass = PlaceRef.class)}))
	private List<PlaceRef> place;

	@Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY_LIST, targetName = "prodSpecCharValueUse")}))
	@Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY_LIST, targetName = "prodSpecCharValueUse", targetClass = ProductSpecificationCharacteristicValueUse.class)}))
	private List<ProductSpecificationCharacteristicValueUse> prodSpecCharValueUse;

	@Getter(onMethod = @__({@AttributeGetter(value = AttributeType.RELATIONSHIP_LIST, targetName = "productOfferingPrice")}))
	@Setter(onMethod = @__({@AttributeSetter(value = AttributeType.RELATIONSHIP_LIST, targetName = "productOfferingPrice", targetClass = ProductOfferingPriceRef.class)}))
	private List<ProductOfferingPriceRef> productOfferingPrice;

	@Getter(onMethod = @__({@AttributeGetter(value = AttributeType.RELATIONSHIP_LIST, targetName = "productOfferingRelationship")}))
	@Setter(onMethod = @__({@AttributeSetter(value = AttributeType.RELATIONSHIP_LIST, targetName = "productOfferingRelationship", targetClass = ProductOfferingRelationship.class)}))
	private List<ProductOfferingRelationship> productOfferingRelationship;

	@Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY_LIST, targetName = "productOfferingTerm")}))
	@Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY_LIST, targetName = "productOfferingTerm", targetClass = ProductOfferingTerm.class)}))
	private List<ProductOfferingTerm> productOfferingTerm;

	@Getter(onMethod = @__({@AttributeGetter(value = AttributeType.RELATIONSHIP, targetName = "productSpecification")}))
	@Setter(onMethod = @__({@AttributeSetter(value = AttributeType.RELATIONSHIP, targetName = "productSpecification", fromProperties = true, targetClass = ProductSpecificationRef.class)}))
	private ProductSpecificationRef productSpecification;

	@Getter(onMethod = @__({@AttributeGetter(value = AttributeType.RELATIONSHIP, targetName = "resourceCandidate")}))
	@Setter(onMethod = @__({@AttributeSetter(value = AttributeType.RELATIONSHIP, targetName = "resourceCandidate", targetClass = ResourceCandidateRef.class)}))
	private ResourceCandidateRef resourceCandidate;

	@Getter(onMethod = @__({@AttributeGetter(value = AttributeType.RELATIONSHIP, targetName = "serviceCandidate")}))
	@Setter(onMethod = @__({@AttributeSetter(value = AttributeType.RELATIONSHIP, targetName = "serviceCandidate", targetClass = ServiceCandidateRef.class)}))
	private ServiceCandidateRef serviceCandidate;

	@Getter(onMethod = @__({@AttributeGetter(value = AttributeType.RELATIONSHIP, targetName = "serviceLevelAgreement")}))
	@Setter(onMethod = @__({@AttributeSetter(value = AttributeType.RELATIONSHIP, targetName = "serviceLevelAgreement", targetClass = SLARef.class)}))
	private SLARef serviceLevelAgreement;

	@Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "validFor")}))
	@Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "validFor")}))
	private TimePeriod validFor;

	public ProductOffering(String id) {
		super(TYPE_PRODUCT_OFFERING, id);
	}

	@Override
	public String getEntityState() {
		return lifecycleStatus;
	}
}
