package org.fiware.resourcecatalog.model;

@jakarta.annotation.Generated("org.openapitools.codegen.languages.MicronautCodegen")
@io.micronaut.core.annotation.Introspected
public class ResourceCatalogChangeEventVO {

	public static final java.lang.String JSON_PROPERTY_EVENT = "event";
	public static final java.lang.String JSON_PROPERTY_EVENT_ID = "eventId";
	public static final java.lang.String JSON_PROPERTY_EVENT_TIME = "eventTime";
	public static final java.lang.String JSON_PROPERTY_EVENT_TYPE = "eventType";
	public static final java.lang.String JSON_PROPERTY_CORRELATION_ID = "correlationId";
	public static final java.lang.String JSON_PROPERTY_DOMAIN = "domain";
	public static final java.lang.String JSON_PROPERTY_TITLE = "title";
	public static final java.lang.String JSON_PROPERTY_DESCRIPTION = "description";
	public static final java.lang.String JSON_PROPERTY_PRIORITY = "priority";
	public static final java.lang.String JSON_PROPERTY_TIME_OCURRED = "timeOcurred";

	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_EVENT)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private ResourceCatalogChangeEventPayloadVO event;

	/** The identifier of the notification. */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_EVENT_ID)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.lang.String eventId;

	/** Time of the event occurrence. */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_EVENT_TIME)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.time.Instant eventTime;

	/** The type of the notification. */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_EVENT_TYPE)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.lang.String eventType;

	/** The correlation id for this event. */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_CORRELATION_ID)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.lang.String correlationId;

	/** The domain of the event. */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_DOMAIN)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.lang.String domain;

	/** The title of the event. */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_TITLE)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.lang.String title;

	/** An explnatory of the event. */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_DESCRIPTION)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.lang.String description;

	/** A priority. */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_PRIORITY)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.lang.String priority;

	/** The time the event occured. */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_TIME_OCURRED)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.time.Instant timeOcurred;

	// methods

	@Override
	public boolean equals(Object object) {
		if (object == this) {
			return true;
		}
		if (object == null || getClass() != object.getClass()) {
			return false;
		}
		ResourceCatalogChangeEventVO other = (ResourceCatalogChangeEventVO) object;
		return java.util.Objects.equals(event, other.event)
				&& java.util.Objects.equals(eventId, other.eventId)
				&& java.util.Objects.equals(eventTime, other.eventTime)
				&& java.util.Objects.equals(eventType, other.eventType)
				&& java.util.Objects.equals(correlationId, other.correlationId)
				&& java.util.Objects.equals(domain, other.domain)
				&& java.util.Objects.equals(title, other.title)
				&& java.util.Objects.equals(description, other.description)
				&& java.util.Objects.equals(priority, other.priority)
				&& java.util.Objects.equals(timeOcurred, other.timeOcurred);
	}

	@Override
	public int hashCode() {
		return java.util.Objects.hash(event, eventId, eventTime, eventType, correlationId, domain, title, description, priority, timeOcurred);
	}

	@Override
	public java.lang.String toString() {
		return new java.lang.StringBuilder()
				.append("ResourceCatalogChangeEventVO[")
				.append("event=").append(event).append(",")
				.append("eventId=").append(eventId).append(",")
				.append("eventTime=").append(eventTime).append(",")
				.append("eventType=").append(eventType).append(",")
				.append("correlationId=").append(correlationId).append(",")
				.append("domain=").append(domain).append(",")
				.append("title=").append(title).append(",")
				.append("description=").append(description).append(",")
				.append("priority=").append(priority).append(",")
				.append("timeOcurred=").append(timeOcurred)
				.append("]")
				.toString();
	}

	// fluent

	public ResourceCatalogChangeEventVO event(ResourceCatalogChangeEventPayloadVO newEvent) {
		this.event = newEvent;
		return this;
	}

	public ResourceCatalogChangeEventVO eventId(java.lang.String newEventId) {
		this.eventId = newEventId;
		return this;
	}

	public ResourceCatalogChangeEventVO eventTime(java.time.Instant newEventTime) {
		this.eventTime = newEventTime;
		return this;
	}

	public ResourceCatalogChangeEventVO eventType(java.lang.String newEventType) {
		this.eventType = newEventType;
		return this;
	}

	public ResourceCatalogChangeEventVO correlationId(java.lang.String newCorrelationId) {
		this.correlationId = newCorrelationId;
		return this;
	}

	public ResourceCatalogChangeEventVO domain(java.lang.String newDomain) {
		this.domain = newDomain;
		return this;
	}

	public ResourceCatalogChangeEventVO title(java.lang.String newTitle) {
		this.title = newTitle;
		return this;
	}

	public ResourceCatalogChangeEventVO description(java.lang.String newDescription) {
		this.description = newDescription;
		return this;
	}

	public ResourceCatalogChangeEventVO priority(java.lang.String newPriority) {
		this.priority = newPriority;
		return this;
	}

	public ResourceCatalogChangeEventVO timeOcurred(java.time.Instant newTimeOcurred) {
		this.timeOcurred = newTimeOcurred;
		return this;
	}

	// getter/setter

	public ResourceCatalogChangeEventPayloadVO getEvent() {
		return event;
	}

	public void setEvent(ResourceCatalogChangeEventPayloadVO newEvent) {
		this.event = newEvent;
	}

	public java.lang.String getEventId() {
		return eventId;
	}

	public void setEventId(java.lang.String newEventId) {
		this.eventId = newEventId;
	}

	public java.time.Instant getEventTime() {
		return eventTime;
	}

	public void setEventTime(java.time.Instant newEventTime) {
		this.eventTime = newEventTime;
	}

	public java.lang.String getEventType() {
		return eventType;
	}

	public void setEventType(java.lang.String newEventType) {
		this.eventType = newEventType;
	}

	public java.lang.String getCorrelationId() {
		return correlationId;
	}

	public void setCorrelationId(java.lang.String newCorrelationId) {
		this.correlationId = newCorrelationId;
	}

	public java.lang.String getDomain() {
		return domain;
	}

	public void setDomain(java.lang.String newDomain) {
		this.domain = newDomain;
	}

	public java.lang.String getTitle() {
		return title;
	}

	public void setTitle(java.lang.String newTitle) {
		this.title = newTitle;
	}

	public java.lang.String getDescription() {
		return description;
	}

	public void setDescription(java.lang.String newDescription) {
		this.description = newDescription;
	}

	public java.lang.String getPriority() {
		return priority;
	}

	public void setPriority(java.lang.String newPriority) {
		this.priority = newPriority;
	}

	public java.time.Instant getTimeOcurred() {
		return timeOcurred;
	}

	public void setTimeOcurred(java.time.Instant newTimeOcurred) {
		this.timeOcurred = newTimeOcurred;
	}
}
