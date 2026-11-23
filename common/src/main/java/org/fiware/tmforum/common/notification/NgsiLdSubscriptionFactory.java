package org.fiware.tmforum.common.notification;

import org.fiware.tmforum.common.domain.subscription.Endpoint;
import org.fiware.tmforum.common.domain.subscription.EntityInfo;
import org.fiware.tmforum.common.domain.subscription.KeyValuePair;
import org.fiware.tmforum.common.domain.subscription.NotificationParams;
import org.fiware.tmforum.common.domain.subscription.Subscription;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Builds the NGSI-LD subscription behind a TMForum hub, shared by the hub controller and the migrator's export so
 * both register the same subscription.
 */
public final class NgsiLdSubscriptionFactory {

	static final String RECEIVER_INFO_LISTENER_ENDPOINT = "Listener-Endpoint";

	private NgsiLdSubscriptionFactory() {
	}

	/**
	 * A subscription with a new random id. The broker notifies {@code notificationEndpoint}, which forwards to the hub's
	 * {@code callback}; null {@code fields} mean every attribute.
	 */
	public static Subscription build(URI notificationEndpoint, String callback, String q, List<String> entityTypes,
			Collection<String> fields) {
		Subscription subscription = new Subscription(UUID.randomUUID().toString());
		subscription.setQ(q);
		subscription.setEntities(entityTypes.stream().map(entityType -> {
			EntityInfo entityInfo = new EntityInfo();
			entityInfo.setType(entityType);
			return entityInfo;
		}).toList());
		NotificationParams notificationParams = new NotificationParams();
		notificationParams.setAttributes(fields == null ? Set.of() : Set.copyOf(fields));
		notificationParams.setFormat(EventConstants.NOTIFICATION_FORMAT);
		notificationParams.setEndpoint(new Endpoint(notificationEndpoint, EventConstants.NOTIFICATION_PAYLOAD_MIME_TYPE,
				List.of(new KeyValuePair(RECEIVER_INFO_LISTENER_ENDPOINT, callback))));
		subscription.setNotification(notificationParams);
		return subscription;
	}

	/**
	 * {@code <serverHost><basepath>/callback}, where a deployment receives the broker's notifications; a null
	 * {@code basepath} counts as empty.
	 */
	public static URI notificationEndpoint(String serverHost, String basepath) {
		String path = basepath != null ? basepath : "";
		if (path.endsWith("/")) {
			path = path.substring(0, path.length() - 1);
		}
		return URI.create(serverHost + path + EventConstants.SUBSCRIPTION_CALLBACK_PATH);
	}
}
