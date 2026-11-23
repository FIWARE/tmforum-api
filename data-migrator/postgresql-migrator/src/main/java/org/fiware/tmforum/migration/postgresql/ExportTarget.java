package org.fiware.tmforum.migration.postgresql;

import org.fiware.tmforum.common.domain.subscription.Subscription;
import org.fiware.tmforum.common.domain.subscription.TMForumSubscription;

import java.net.URI;
import java.util.Optional;

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
	 * The broker's hub with the id, read through the domain mapping; empty when the broker holds none.
	 */
	Optional<TMForumSubscription> hub(URI id);

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
