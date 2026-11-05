package org.fiware.tmforum.product;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.fiware.tmforum.common.domain.Entity;
import org.fiware.tmforum.common.domain.RefEntity;
import org.fiware.tmforum.common.validation.ReferencedEntity;
import io.github.wistefan.mapping.annotations.AttributeGetter;
import io.github.wistefan.mapping.annotations.AttributeSetter;
import io.github.wistefan.mapping.annotations.AttributeType;
import io.github.wistefan.mapping.annotations.DatasetId;
import io.github.wistefan.mapping.annotations.RelationshipObject;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@EqualsAndHashCode(callSuper = true)
public class RelatedProductOrderItemRef extends Entity implements ReferencedEntity {

	private static final String NO_ORDER_ITEM_ID = "item";

	// the relationship object: written through getEntityId and read through the String constructor. The accessors
	// serve the VO mapping and the plain-JSON form wherever the ref is embedded as a value (e.g. ProductRefOrValue)
	@Getter
	@Setter
	private URI productOrderId;

	@Getter(onMethod = @__({
			@AttributeGetter(value = AttributeType.PROPERTY, targetName = "productOrderHref", embedProperty = true) }))
	@Setter(onMethod = @__({
			@AttributeSetter(value = AttributeType.PROPERTY, targetName = "productOrderHref", fromProperties = true, targetClass = URI.class) }))
	private URI productOrderHref;

	@Getter(onMethod = @__({
			@AttributeGetter(value = AttributeType.PROPERTY, targetName = "orderItemId", embedProperty = true) }))
	@Setter(onMethod = @__({
			@AttributeSetter(value = AttributeType.PROPERTY, targetName = "orderItemId", fromProperties = true) }))
	private String orderItemId;

	@Getter(onMethod = @__({
			@AttributeGetter(value = AttributeType.PROPERTY, targetName = "orderItemAction", embedProperty = true) }))
	@Setter(onMethod = @__({
			@AttributeSetter(value = AttributeType.PROPERTY, targetName = "orderItemAction", fromProperties = true) }))
	private String orderItemAction;

	@Getter(onMethod = @__({
			@AttributeGetter(value = AttributeType.PROPERTY, targetName = "role", embedProperty = true) }))
	@Setter(onMethod = @__({
			@AttributeSetter(value = AttributeType.PROPERTY, targetName = "role", fromProperties = true) }))
	private String role;

	@Getter(onMethod = @__({
			@AttributeGetter(value = AttributeType.PROPERTY, targetName = "atReferredType", embedProperty = true) }))
	@Setter(onMethod = @__({
			@AttributeSetter(value = AttributeType.PROPERTY, targetName = "atReferredType", fromProperties = true,
					legacyNames = { "@referredType" }) }))
	private String atReferredType;

	public RelatedProductOrderItemRef() {
	}

	/** Used by the NGSI-LD mapping to build the ref from its relationship object, the product order id. */
	public RelatedProductOrderItemRef(String productOrderId) {
		this.productOrderId = URI.create(productOrderId);
	}

	@Override public List<String> getReferencedTypes() {
		return new ArrayList<>(List.of("product-order"));
	}

	@RelationshipObject
	@Override
	public URI getEntityId() {
		return productOrderId;
	}

	/** Includes the orderItemId, so that several items of one product order stay distinct instances. */
	@JsonIgnore
	@DatasetId
	public URI getDatasetId() {
		String orderItem = Optional.ofNullable(orderItemId).orElse(NO_ORDER_ITEM_ID);
		return URI.create(String.format("%s:%s", productOrderId, URLEncoder.encode(orderItem, StandardCharsets.UTF_8)));
	}

}
