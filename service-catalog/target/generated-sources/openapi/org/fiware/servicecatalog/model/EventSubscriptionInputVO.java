package org.fiware.servicecatalog.model;

@jakarta.annotation.Generated("org.openapitools.codegen.languages.MicronautCodegen")
@io.micronaut.core.annotation.Introspected
public class EventSubscriptionInputVO {

	public static final java.lang.String JSON_PROPERTY_CALLBACK = "callback";
	public static final java.lang.String JSON_PROPERTY_QUERY = "query";

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
		EventSubscriptionInputVO other = (EventSubscriptionInputVO) object;
		return java.util.Objects.equals(callback, other.callback)
				&& java.util.Objects.equals(query, other.query);
	}

	@Override
	public int hashCode() {
		return java.util.Objects.hash(callback, query);
	}

	@Override
	public java.lang.String toString() {
		return new java.lang.StringBuilder()
				.append("EventSubscriptionInputVO[")
				.append("callback=").append(callback).append(",")
				.append("query=").append(query)
				.append("]")
				.toString();
	}

	// fluent

	public EventSubscriptionInputVO callback(java.lang.String newCallback) {
		this.callback = newCallback;
		return this;
	}

	public EventSubscriptionInputVO query(java.lang.String newQuery) {
		this.query = newQuery;
		return this;
	}

	// getter/setter

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
