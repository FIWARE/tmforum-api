package org.fiware.tmforum.migration.postgresql;

import org.fiware.tmforum.common.domain.subscription.Subscription;

import java.net.URI;

/**
 * Where {@code export} writes to: the broker's entities and its NGSI-LD subscriptions; {@link BrokerExportTarget} in
 * production, a fake in unit tests.
 */
interface ExportTarget extends SubscriptionStore {

	/**
	 * The broker's count of the type.
	 */
	long count(String type);

	/**
	 * Whether the broker holds an entity with the id.
	 */
	boolean exists(URI id);

	/**
	 * Creates the entity like the NGSI-LD adapter does, with the broker's own timestamps. Throws a
	 * {@link org.fiware.tmforum.storage.StorageException} with reason {@code CONFLICT} when the id is taken.
	 */
	void create(Object domainEntity);

	/**
	 * Registers the subscription with the tenant and {@code @context} the hub controller uses.
	 */
	void registerSubscription(Subscription subscription);
}
