package org.fiware.tmforum.productordering.domain;

import io.github.wistefan.mapping.annotations.AttributeGetter;
import io.github.wistefan.mapping.annotations.AttributeSetter;
import io.github.wistefan.mapping.annotations.AttributeType;
import io.github.wistefan.mapping.annotations.MappingEnabled;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.fiware.tmforum.common.domain.EntityWithId;

import java.net.URI;
import java.time.Instant;

@EqualsAndHashCode(callSuper = true)
@MappingEnabled(entityType = CancelProductOrder.TYPE_CANCEL_PRODUCT_ORDER)
public class CancelProductOrder extends EntityWithId {

	public static final String TYPE_CANCEL_PRODUCT_ORDER = "cancel-product-order";

	@Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "href") }))
	@Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "href") }))
	private URI href;

	@Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "cancellationReason") }))
	@Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "cancellationReason") }))
	private String cancellationReason;

	@Getter(onMethod = @__({
			@AttributeGetter(value = AttributeType.PROPERTY, targetName = "effectiveCancellationDate") }))
	@Setter(onMethod = @__({
			@AttributeSetter(value = AttributeType.PROPERTY, targetName = "effectiveCancellationDate") }))
	private Instant effectiveCancellationDate;

	@Getter(onMethod = @__({
			@AttributeGetter(value = AttributeType.PROPERTY, targetName = "requestedCancellationDate") }))
	@Setter(onMethod = @__({
			@AttributeSetter(value = AttributeType.PROPERTY, targetName = "requestedCancellationDate") }))
	private Instant requestedCancellationDate;

	@Getter(onMethod = @__({
			@AttributeGetter(value = AttributeType.RELATIONSHIP, targetName = "productOrder", embedProperty = true) }))
	@Setter(onMethod = @__({
			@AttributeSetter(value = AttributeType.RELATIONSHIP, targetName = "productOrder", fromProperties = true, targetClass = ProductOrderRef.class) }))
	private ProductOrderRef productOrder;

	@Getter(onMethod = @__({
			@AttributeGetter(value = AttributeType.PROPERTY, targetName = "state") }))
	@Setter(onMethod = @__({
			@AttributeSetter(value = AttributeType.PROPERTY, targetName = "state") }))
	private TaskState state;

	public CancelProductOrder(String id) {
		super(TYPE_CANCEL_PRODUCT_ORDER, id);
	}

	@Override
	public String getEntityState() {
		return state != null ? state.getValue() : null;
	}
}
