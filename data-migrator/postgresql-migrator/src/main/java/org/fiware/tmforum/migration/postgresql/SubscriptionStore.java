package org.fiware.tmforum.migration.postgresql;

import java.net.URI;
import java.util.List;

/**
 * The broker's NGSI-LD subscriptions; {@link BrokerSource} in production, a fake in unit tests.
 */
interface SubscriptionStore {

	/**
	 * Every subscription of the tenant, read in pages of {@code pageSize}.
	 */
	List<BrokerSubscription> subscriptions(int pageSize);

	/**
	 * Deletes the subscription. Returns false when the broker holds none with that id; throws on any other failure.
	 */
	boolean deleteSubscription(URI subscriptionId);

	/**
	 * {@code endpoint} is the notification endpoint's URI, null when the broker returned none.
	 */
	record BrokerSubscription(URI id, String endpoint) {
	}
}
