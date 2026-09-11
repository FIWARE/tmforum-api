package org.fiware.tmforum.common.domain.subscription;

import io.github.wistefan.mapping.annotations.AttributeGetter;
import io.github.wistefan.mapping.annotations.AttributeSetter;
import io.github.wistefan.mapping.annotations.AttributeType;
import io.github.wistefan.mapping.annotations.MappingEnabled;
import io.micronaut.context.annotation.Factory;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.fiware.tmforum.common.domain.EntityWithId;
import org.fiware.tmforum.common.mapping.ForbiddenCharacterEscaper;

import java.net.URI;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@MappingEnabled(entityType = TMForumSubscription.TYPE_TM_FORUM_SUBSCRIPTION)
public class TMForumSubscription extends EntityWithId {
	public static final String TYPE_TM_FORUM_SUBSCRIPTION = "tm-forum-subscription";

	@Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "callback")}))
	@Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "callback")}))
	private URI callback;

	private String query;

	private String rawQuery;

	@Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY_LIST, targetName = "eventTypes")}))
	@Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY_LIST, targetName = "eventTypes", targetClass = String.class)}))
	private List<String> eventTypes;

	@Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY_LIST, targetName = "entities")}))
	@Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY_LIST, targetName = "entities", targetClass = String.class)}))
	private List<String> entities;

	@Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY_LIST, targetName = "fields")}))
	@Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY_LIST, targetName = "fields", targetClass = String.class)}))
	private List<String> fields;

	@Getter(onMethod = @__({@AttributeGetter(value = AttributeType.RELATIONSHIP, targetName = "subscription")}))
	@Setter(onMethod = @__({@AttributeSetter(value = AttributeType.RELATIONSHIP, targetName = "subscription", targetClass = Subscription.class, fromProperties = true)}))
	private Subscription subscription;

	/**
	 * Empty constructor for cache serialization and deserialization
	 */
	public TMForumSubscription() {
		super(TYPE_TM_FORUM_SUBSCRIPTION, null);
	}

	public TMForumSubscription(String id) {
		super(TYPE_TM_FORUM_SUBSCRIPTION, id);
	}

	public String getQuery() {
		return query;
	}

	public void setQuery(String query) {
		this.query = query;
	}

	public String getRawQuery() {
		return rawQuery;
	}

	public void setRawQuery(String rawQuery) {
		this.rawQuery = rawQuery;
	}

	// query/rawQuery are inherently key=value-shaped and would otherwise contain NGSI-LD's
	// "Forbidden Characters" (ETSI GS CIM 009 clause 4.6.4, e.g. '='), which some brokers reject.
	// These escaped variants are what gets sent to/read from the broker; getQuery()/setQuery()
	// and getRawQuery()/setRawQuery() above keep working with the original, human-readable value.

	@AttributeGetter(value = AttributeType.PROPERTY, targetName = "query")
	public String getEscapedQuery() {
		return ForbiddenCharacterEscaper.escape(query);
	}

	@AttributeSetter(value = AttributeType.PROPERTY, targetName = "query")
	public void setEscapedQuery(String escapedQuery) {
		this.query = ForbiddenCharacterEscaper.unescape(escapedQuery);
	}

	@AttributeGetter(value = AttributeType.PROPERTY, targetName = "rawQuery")
	public String getEscapedRawQuery() {
		return ForbiddenCharacterEscaper.escape(rawQuery);
	}

	@AttributeSetter(value = AttributeType.PROPERTY, targetName = "rawQuery")
	public void setEscapedRawQuery(String escapedRawQuery) {
		this.rawQuery = ForbiddenCharacterEscaper.unescape(escapedRawQuery);
	}
}
