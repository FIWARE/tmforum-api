package org.fiware.tmforum.serviceordering.domain;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.fiware.tmforum.common.domain.EntityWithId;
import org.fiware.tmforum.common.domain.RelatedParty;
import io.github.wistefan.mapping.annotations.AttributeGetter;
import io.github.wistefan.mapping.annotations.AttributeSetter;
import io.github.wistefan.mapping.annotations.AttributeType;
import io.github.wistefan.mapping.annotations.MappingEnabled;
import org.fiware.tmforum.resource.Note;

import java.net.URI;
import java.time.Instant;
import java.util.List;

/**
 * A Service Order is a request to provision a set of services, triggered by a customer or an internal process.
 * It follows the TMF641 Service Order Management lifecycle.
 */
@EqualsAndHashCode(callSuper = true)
@MappingEnabled(entityType = ServiceOrder.TYPE_SERVICE_ORDER)
public class ServiceOrder extends EntityWithId {

	/** The NGSI-LD entity type for service orders. */
	public static final String TYPE_SERVICE_ORDER = "service-order";

	@Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "href") }))
	@Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "href") }))
	private URI href;

	@Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "cancellationDate") }))
	@Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "cancellationDate") }))
	private Instant cancellationDate;

	@Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "cancellationReason") }))
	@Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "cancellationReason") }))
	private String cancellationReason;

	@Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "category") }))
	@Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "category") }))
	private String category;

	@Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "completionDate") }))
	@Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "completionDate") }))
	private Instant completionDate;

	@Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "description") }))
	@Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "description") }))
	private String description;

	@Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "expectedCompletionDate") }))
	@Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "expectedCompletionDate") }))
	private Instant expectedCompletionDate;

	@Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "externalId") }))
	@Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "externalId") }))
	private String externalId;

	@Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "notificationContact") }))
	@Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "notificationContact") }))
	private String notificationContact;

	@Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "orderDate") }))
	@Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "orderDate") }))
	private Instant orderDate;

	@Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "priority") }))
	@Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "priority") }))
	private String priority;

	@Getter(onMethod = @__({
			@AttributeGetter(value = AttributeType.PROPERTY, targetName = "requestedCompletionDate") }))
	@Setter(onMethod = @__({
			@AttributeSetter(value = AttributeType.PROPERTY, targetName = "requestedCompletionDate") }))
	private Instant requestedCompletionDate;

	@Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "requestedStartDate") }))
	@Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "requestedStartDate") }))
	private Instant requestedStartDate;

	@Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "startDate") }))
	@Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "startDate") }))
	private Instant startDate;

	@Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "state") }))
	@Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "state") }))
	private ServiceOrderState state;

	@Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY_LIST, targetName = "externalReference") }))
	@Setter(onMethod = @__({
			@AttributeSetter(value = AttributeType.PROPERTY_LIST, targetName = "externalReference", targetClass = ExternalReference.class) }))
	private List<ExternalReference> externalReference;

	@Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY_LIST, targetName = "note") }))
	@Setter(onMethod = @__({
			@AttributeSetter(value = AttributeType.PROPERTY_LIST, targetName = "note", targetClass = Note.class) }))
	private List<Note> note;

	@Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY_LIST, targetName = "errorMessage") }))
	@Setter(onMethod = @__({
			@AttributeSetter(value = AttributeType.PROPERTY_LIST, targetName = "errorMessage", targetClass = ServiceOrderErrorMessage.class) }))
	private List<ServiceOrderErrorMessage> errorMessage;

	@Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY_LIST, targetName = "jeopardyAlert") }))
	@Setter(onMethod = @__({
			@AttributeSetter(value = AttributeType.PROPERTY_LIST, targetName = "jeopardyAlert", targetClass = ServiceOrderJeopardyAlert.class) }))
	private List<ServiceOrderJeopardyAlert> jeopardyAlert;

	@Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY_LIST, targetName = "milestone") }))
	@Setter(onMethod = @__({
			@AttributeSetter(value = AttributeType.PROPERTY_LIST, targetName = "milestone", targetClass = ServiceOrderMilestone.class) }))
	private List<ServiceOrderMilestone> milestone;

	@Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY_LIST, targetName = "orderRelationship") }))
	@Setter(onMethod = @__({
			@AttributeSetter(value = AttributeType.PROPERTY_LIST, targetName = "orderRelationship", targetClass = ServiceOrderRelationship.class) }))
	private List<ServiceOrderRelationship> orderRelationship;

	@Getter(onMethod = @__({
			@AttributeGetter(value = AttributeType.PROPERTY_LIST, targetName = "serviceOrderItem", embedProperty = true) }))
	@Setter(onMethod = @__({
			@AttributeSetter(value = AttributeType.PROPERTY_LIST, targetName = "serviceOrderItem", fromProperties = true, targetClass = ServiceOrderItem.class) }))
	private List<ServiceOrderItem> serviceOrderItem;

	@Getter(onMethod = @__({
			@AttributeGetter(value = AttributeType.RELATIONSHIP_LIST, targetName = "relatedParty", embedProperty = true) }))
	@Setter(onMethod = @__({
			@AttributeSetter(value = AttributeType.RELATIONSHIP_LIST, targetName = "relatedParty", fromProperties = true, targetClass = RelatedParty.class) }))
	private List<RelatedParty> relatedParty;

	/**
	 * Creates a new service order with the given NGSI-LD id.
	 *
	 * @param id the NGSI-LD id of the service order
	 */
	public ServiceOrder(String id) {
		super(TYPE_SERVICE_ORDER, id);
	}

	@Override
	public String getEntityState() {
		return state != null ? state.getValue() : null;
	}
}
