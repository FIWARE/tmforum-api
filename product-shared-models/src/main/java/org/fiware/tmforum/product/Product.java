package org.fiware.tmforum.product;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.fiware.tmforum.common.domain.BillingAccountRef;
import org.fiware.tmforum.common.domain.EntityWithId;
import org.fiware.tmforum.common.domain.RelatedParty;
import io.github.wistefan.mapping.annotations.AttributeGetter;
import io.github.wistefan.mapping.annotations.AttributeSetter;
import io.github.wistefan.mapping.annotations.AttributeType;
import io.github.wistefan.mapping.annotations.MappingEnabled;
import org.fiware.tmforum.resource.ResourceRef;
import org.fiware.tmforum.service.ServiceRef;

import java.net.URI;
import java.time.Instant;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@MappingEnabled(entityType = Product.TYPE_PRODUCT)
public class Product extends EntityWithId {

	public static final String TYPE_PRODUCT = "product";

	@Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "href") }))
	@Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "href") }))
	private URI href;

	@Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "description") }))
	@Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "description") }))
	private String description;

	@Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "isBundle") }))
	@Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "isBundle") }))
	private Boolean isBundle;

	@Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "isCustomerVisible") }))
	@Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "isCustomerVisible") }))
	private Boolean isCustomerVisible;

	@Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "name") }))
	@Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "name") }))
	private String name;

	@Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "orderDate") }))
	@Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "orderDate") }))
	private Instant orderDate;

	@Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "productSerialNumber") }))
	@Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "productSerialNumber") }))
	private String productSerialNumber;

	@Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "startDate") }))
	@Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "startDate") }))
	private Instant startDate;

	@Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "terminationDate") }))
	@Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "terminationDate") }))
	private Instant terminationDate;

	@Getter(onMethod = @__({
			@AttributeGetter(value = AttributeType.RELATIONSHIP_LIST, targetName = "agreement", embedProperty = true) }))
	@Setter(onMethod = @__({
			@AttributeSetter(value = AttributeType.RELATIONSHIP_LIST, targetName = "agreement", fromProperties = true, targetClass = AgreementItemRef.class) }))
	private List<AgreementItemRef> agreement;

	@Getter(onMethod = @__({
			@AttributeGetter(value = AttributeType.RELATIONSHIP, targetName = "billingAccount", embedProperty = true) }))
	@Setter(onMethod = @__({
			@AttributeSetter(value = AttributeType.RELATIONSHIP, targetName = "billingAccount", fromProperties = true, targetClass = BillingAccountRef.class) }))
	private BillingAccountRef billingAccount;

	@Getter(onMethod = @__({
			@AttributeGetter(value = AttributeType.RELATIONSHIP_LIST, targetName = "place", embedProperty = true) }))
	@Setter(onMethod = @__({
			@AttributeSetter(value = AttributeType.RELATIONSHIP_LIST, targetName = "place", fromProperties = true, targetClass = RelatedPlaceRef.class) }))
	private List<RelatedPlaceRef> place;

	@Getter(onMethod = @__({
			@AttributeGetter(value = AttributeType.RELATIONSHIP_LIST, targetName = "product", embedProperty = true) }))
	@Setter(onMethod = @__({
			@AttributeSetter(value = AttributeType.RELATIONSHIP_LIST, targetName = "product", fromProperties = true, targetClass = ProductRef.class) }))
	private List<ProductRef> product;

	@Getter(onMethod = @__({
			@AttributeGetter(value = AttributeType.PROPERTY_LIST, targetName = "productCharacteristic") }))
	@Setter(onMethod = @__({
			@AttributeSetter(value = AttributeType.PROPERTY_LIST, targetName = "productCharacteristic", targetClass = Characteristic.class) }))
	private List<Characteristic> productCharacteristic;

	@Getter(onMethod = @__({
			@AttributeGetter(value = AttributeType.RELATIONSHIP, targetName = "productOffering", embedProperty = true) }))
	@Setter(onMethod = @__({
			@AttributeSetter(value = AttributeType.RELATIONSHIP, targetName = "productOffering", fromProperties = true, targetClass = ProductOfferingRef.class) }))
	private ProductOfferingRef productOffering;

	@Getter(onMethod = @__({
			@AttributeGetter(value = AttributeType.RELATIONSHIP_LIST, targetName = "productOrderItem", embedProperty = true) }))
	@Setter(onMethod = @__({
			@AttributeSetter(value = AttributeType.RELATIONSHIP_LIST, targetName = "productOrderItem", fromProperties = true, targetClass = RelatedProductOrderItemRef.class) }))
	private List<RelatedProductOrderItemRef> productOrderItem;

	@Getter(onMethod = @__({
			@AttributeGetter(value = AttributeType.PROPERTY_LIST, targetName = "productPrice") }))
	@Setter(onMethod = @__({
			@AttributeSetter(value = AttributeType.PROPERTY_LIST, targetName = "productPrice", targetClass = ProductPrice.class) }))
	private List<ProductPrice> productPrice;

	@Getter(onMethod = @__({
			@AttributeGetter(value = AttributeType.PROPERTY_LIST, targetName = "productRelationship") }))
	@Setter(onMethod = @__({
			@AttributeSetter(value = AttributeType.PROPERTY_LIST, targetName = "productRelationship", targetClass = ProductRelationship.class) }))
	private List<ProductRelationship> productRelationship;

	@Getter(onMethod = @__({
			@AttributeGetter(value = AttributeType.RELATIONSHIP, targetName = "productSpecification", embedProperty = true) }))
	@Setter(onMethod = @__({
			@AttributeSetter(value = AttributeType.RELATIONSHIP, targetName = "productSpecification", fromProperties = true, targetClass = ProductSpecificationRef.class) }))
	private ProductSpecificationRef productSpecification;

	@Getter(onMethod = @__({
			@AttributeGetter(value = AttributeType.PROPERTY_LIST, targetName = "productTerm") }))
	@Setter(onMethod = @__({
			@AttributeSetter(value = AttributeType.PROPERTY_LIST, targetName = "productTerm", targetClass = ProductTerm.class) }))
	private List<ProductTerm> productTerm;

	@Getter(onMethod = @__({
			@AttributeGetter(value = AttributeType.RELATIONSHIP_LIST, targetName = "realizingResource", embedProperty = true) }))
	@Setter(onMethod = @__({
			@AttributeSetter(value = AttributeType.RELATIONSHIP_LIST, targetName = "realizingResource", fromProperties = true, targetClass = ResourceRef.class) }))
	private List<ResourceRef> realizingResource;

	@Getter(onMethod = @__({
			@AttributeGetter(value = AttributeType.RELATIONSHIP_LIST, targetName = "realizingService", embedProperty = true) }))
	@Setter(onMethod = @__({
			@AttributeSetter(value = AttributeType.RELATIONSHIP_LIST, targetName = "realizingService", fromProperties = true, targetClass = ServiceRef.class) }))
	private List<ServiceRef> realizingService;

	@Getter(onMethod = @__({
			@AttributeGetter(value = AttributeType.RELATIONSHIP_LIST, targetName = "relatedParty", embedProperty = true) }))
	@Setter(onMethod = @__({
			@AttributeSetter(value = AttributeType.RELATIONSHIP_LIST, targetName = "relatedParty", fromProperties = true, targetClass = RelatedParty.class) }))
	private List<RelatedParty> relatedParty;

	@Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "status") }))
	@Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "status") }))
	private ProductStatusType status;

	public Product(String id) {
		super(TYPE_PRODUCT, id);
	}

	@Override
	public String getEntityState() {
		return status != null ? status.getValue() : null;
	}
}
