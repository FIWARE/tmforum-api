package org.fiware.tmforum.serviceordering.domain;

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

/**
 * Request for cancellation of an existing service order.
 */
@EqualsAndHashCode(callSuper = true)
@MappingEnabled(entityType = CancelServiceOrder.TYPE_CANCEL_SERVICE_ORDER)
public class CancelServiceOrder extends EntityWithId {

	/** The NGSI-LD entity type for cancel service orders. */
	public static final String TYPE_CANCEL_SERVICE_ORDER = "cancel-service-order";

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
			@AttributeGetter(value = AttributeType.RELATIONSHIP, targetName = "serviceOrder", embedProperty = true) }))
	@Setter(onMethod = @__({
			@AttributeSetter(value = AttributeType.RELATIONSHIP, targetName = "serviceOrder", fromProperties = true, targetClass = ServiceOrderRef.class) }))
	private ServiceOrderRef serviceOrder;

	@Getter(onMethod = @__({
			@AttributeGetter(value = AttributeType.PROPERTY, targetName = "state") }))
	@Setter(onMethod = @__({
			@AttributeSetter(value = AttributeType.PROPERTY, targetName = "state") }))
	private TaskState state;

	/**
	 * Creates a new cancel service order with the given NGSI-LD id.
	 *
	 * @param id the NGSI-LD id of the cancel service order
	 */
	public CancelServiceOrder(String id) {
		super(TYPE_CANCEL_SERVICE_ORDER, id);
	}

	@Override
	public String getEntityState() {
		return state != null ? state.getValue() : null;
	}
}
