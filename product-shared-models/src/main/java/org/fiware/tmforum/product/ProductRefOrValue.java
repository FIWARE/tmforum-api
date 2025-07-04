package org.fiware.tmforum.product;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.fiware.tmforum.common.domain.BillingAccountRef;
import org.fiware.tmforum.common.domain.Entity;
import org.fiware.tmforum.common.domain.RelatedParty;
import org.fiware.tmforum.common.domain.RelatedPartyRefValue;
import org.fiware.tmforum.common.validation.ReferencedEntity;
import org.fiware.tmforum.resource.ResourceRef;
import org.fiware.tmforum.service.ServiceRef;

import io.github.wistefan.mapping.annotations.MappingEnabled;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
//@MappingEnabled(entityType = Product.TYPE_PRODUCT)
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class ProductRefOrValue extends Entity implements ReferencedEntity {

	private URI id;
	private URI href;
	private String description;
	private Boolean isBundle;
	private Boolean isCustomerVisible;
	private String name;
	private Instant orderDate;
	private String productSerialNumber;
	private Instant startDate;
	private Instant terminationDate;
	private List<AgreementItemRef> agreement;
	private BillingAccountRef billingAccount;
	private List<RelatedPlaceRef> place;
	private List<ProductRef> product;
	private List<Characteristic> productCharacteristic;
	private ProductOfferingRef productOffering;
	private List<RelatedProductOrderItemRef> productOrderItem;
	private List<ProductPriceValue> productPrice;
	private List<ProductRelationship> productRelationship;
	private ProductSpecificationRef productSpecification;
	private List<ProductTerm> productTerm;
	private List<ResourceRef> realizingResource;
	private List<ServiceRef> realizingService;
	private List<RelatedParty> relatedParty;
	private ProductStatusType status;

	@Override
	public List<String> getReferencedTypes() {
		return new ArrayList<>(List.of("product"));
	}

	@Override
	public URI getEntityId() {
		return this.id;
	}
}
