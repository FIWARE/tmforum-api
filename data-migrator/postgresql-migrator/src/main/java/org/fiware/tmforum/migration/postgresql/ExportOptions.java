package org.fiware.tmforum.migration.postgresql;

import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Switches of the {@code export} command.
 *
 * @param resume               accept a non-empty broker: an id already stored and a hub whose stored subscription is
 *                             registered count as skipped
 * @param notificationEndpoint the broker deployment's {@code <serverHost><basepath>/callback}; null when not given
 * @param endpointsByType      overrides of {@code notificationEndpoint} per entity type a hub listens to
 */
record ExportOptions(int pageSize, boolean resume, boolean failFast, boolean dryRun, URI notificationEndpoint,
		Map<String, URI> endpointsByType) {

	ExportOptions {
		if (pageSize < 1) {
			throw new IllegalArgumentException("The page size must be positive, was " + pageSize + ".");
		}
		endpointsByType = endpointsByType == null ? Map.of() : Map.copyOf(endpointsByType);
	}

	/**
	 * Every configured notification endpoint, as compared with the broker's subscriptions.
	 */
	Set<String> endpoints() {
		Set<String> endpoints = new HashSet<>();
		if (notificationEndpoint != null) {
			endpoints.add(notificationEndpoint.toString());
		}
		endpointsByType.values().forEach(endpoint -> endpoints.add(endpoint.toString()));
		return endpoints;
	}

	/**
	 * The endpoint of a hub listening to {@code entityTypes}. Throws {@link IllegalArgumentException} when a type has
	 * none or the types map to different endpoints, since one subscription notifies one deployment.
	 */
	URI notificationEndpoint(List<String> entityTypes) {
		List<String> types = entityTypes == null ? List.of() : entityTypes;
		if (types.isEmpty()) {
			return requireDefault(types);
		}
		Set<URI> endpoints = types.stream()
				.map(type -> endpointsByType.getOrDefault(type, notificationEndpoint))
				.collect(Collectors.toSet());
		if (endpoints.contains(null)) {
			return requireDefault(types.stream().filter(type -> !endpointsByType.containsKey(type)).toList());
		}
		if (endpoints.size() > 1) {
			throw new IllegalArgumentException(String.format("the hub listens to %s, which notify different endpoints "
					+ "%s; one NGSI-LD subscription can only notify one.", types, endpoints));
		}
		return endpoints.iterator().next();
	}

	private URI requireDefault(List<String> types) {
		if (notificationEndpoint == null) {
			throw new IllegalArgumentException(String.format("no notification endpoint for the hub's entity types %s; "
					+ "set --notification-endpoint or --notification-endpoint-for.", types));
		}
		return notificationEndpoint;
	}
}
