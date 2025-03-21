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

import java.net.URI;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@MappingEnabled(entityType = TMForumSubscription.TYPE_TM_FORUM_SUBSCRIPTION)
public class TMForumSubscription extends EntityWithId {
	public static final String TYPE_TM_FORUM_SUBSCRIPTION = "tm-forum-subscription";

	@Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "callback")}))
	@Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "callback")}))
	private URI callback;

	@Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "query")}))
	@Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "query")}))
	private String query;

	@Getter(onMethod = @__({@AttributeGetter(value = AttributeType.PROPERTY, targetName = "rawQuery")}))
	@Setter(onMethod = @__({@AttributeSetter(value = AttributeType.PROPERTY, targetName = "rawQuery")}))
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
}
