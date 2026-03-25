package org.fiware.tmforum.resourceordering.domain;

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
 * A Resource Order is a request to provision a set of Resources (logical and physical)
 * triggered by the request to provision a Service through a Service Order.
 */
@EqualsAndHashCode(callSuper = true)
@MappingEnabled(entityType = ResourceOrder.TYPE_RESOURCE_ORDER)
public class ResourceOrder extends EntityWithId {

	public static final String TYPE_RESOURCE_ORDER = "resource-order";

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

	@Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "name") }))
	@Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "name") }))
	private String name;

	@Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "orderDate") }))
	@Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "orderDate") }))
	private Instant orderDate;

	@Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "orderType") }))
	@Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "orderType") }))
	private String orderType;

	@Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "priority") }))
	@Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "priority") }))
	private Integer priority;

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

	@Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY_LIST, targetName = "externalReference") }))
	@Setter(onMethod = @__({
			@AttributeSetter(value = AttributeType.PROPERTY_LIST, targetName = "externalReference", targetClass = ExternalReference.class) }))
	private List<ExternalReference> externalReference;

	@Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY_LIST, targetName = "note") }))
	@Setter(onMethod = @__({
			@AttributeSetter(value = AttributeType.PROPERTY_LIST, targetName = "note", targetClass = Note.class) }))
	private List<Note> note;

	@Getter(onMethod = @__({
			@AttributeGetter(value = AttributeType.PROPERTY_LIST, targetName = "orderItem", embedProperty = true) }))
	@Setter(onMethod = @__({
			@AttributeSetter(value = AttributeType.PROPERTY_LIST, targetName = "orderItem", fromProperties = true, targetClass = ResourceOrderItem.class) }))
	private List<ResourceOrderItem> orderItem;

	@Getter(onMethod = @__({
			@AttributeGetter(value = AttributeType.RELATIONSHIP_LIST, targetName = "relatedParty", embedProperty = true) }))
	@Setter(onMethod = @__({
			@AttributeSetter(value = AttributeType.RELATIONSHIP_LIST, targetName = "relatedParty", fromProperties = true, targetClass = RelatedParty.class) }))
	private List<RelatedParty> relatedParty;

	@Getter(onMethod = @__({ @AttributeGetter(value = AttributeType.PROPERTY, targetName = "state") }))
	@Setter(onMethod = @__({ @AttributeSetter(value = AttributeType.PROPERTY, targetName = "state") }))
	private ResourceOrderState state;

	public ResourceOrder(String id) {
		super(TYPE_RESOURCE_ORDER, id);
	}

	@Override
	public String getEntityState() {
		return state != null ? state.getValue() : null;
	}
}
