package org.fiware.servicecatalog.model;

@jakarta.annotation.Generated("org.openapitools.codegen.languages.MicronautCodegen")
@io.micronaut.core.annotation.Introspected
public class EventSubscriptionVO {

	public static final java.lang.String JSON_PROPERTY_ID = "id";
	public static final java.lang.String JSON_PROPERTY_CALLBACK = "callback";
	public static final java.lang.String JSON_PROPERTY_QUERY = "query";

	/** Id of the listener */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_ID)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.ALWAYS)
	private java.lang.String id;

	/** The callback being registered. */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_CALLBACK)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.ALWAYS)
	private java.lang.String callback;

	/** additional data to be passed */
	@com.fasterxml.jackson.annotation.JsonProperty(JSON_PROPERTY_QUERY)
	@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
	private java.lang.String query;

	// methods

	@Override
	public boolean equals(Object object) {
		if (object == this) {
			return true;
		}
		if (object == null || getClass() != object.getClass()) {
			return false;
		}
		EventSubscriptionVO other = (EventSubscriptionVO) object;
		return java.util.Objects.equals(id, other.id)
				&& java.util.Objects.equals(callback, other.callback)
				&& java.util.Objects.equals(query, other.query);
	}

	@Override
	public int hashCode() {
		return java.util.Objects.hash(id, callback, query);
	}

	@Override
	public java.lang.String toString() {
		return new java.lang.StringBuilder()
				.append("EventSubscriptionVO[")
				.append("id=").append(id).append(",")
				.append("callback=").append(callback).append(",")
				.append("query=").append(query)
				.append("]")
				.toString();
	}

	// fluent

	public EventSubscriptionVO id(java.lang.String newId) {
		this.id = newId;
		return this;
	}

	public EventSubscriptionVO callback(java.lang.String newCallback) {
		this.callback = newCallback;
		return this;
	}

	public EventSubscriptionVO query(java.lang.String newQuery) {
		this.query = newQuery;
		return this;
	}

	// getter/setter

	public java.lang.String getId() {
		return id;
	}

	public void setId(java.lang.String newId) {
		this.id = newId;
	}

	public java.lang.String getCallback() {
		return callback;
	}

	public void setCallback(java.lang.String newCallback) {
		this.callback = newCallback;
	}

	public java.lang.String getQuery() {
		return query;
	}

	public void setQuery(java.lang.String newQuery) {
		this.query = newQuery;
	}
}
