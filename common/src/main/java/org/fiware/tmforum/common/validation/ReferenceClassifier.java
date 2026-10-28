package org.fiware.tmforum.common.validation;

import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.configuration.ReferenceProperties;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Classifies references as local or external. A reference is external when its href is absolute and names a host
 * other than this deployment's ({@code general.serverHost}, {@code references.local-hosts}); otherwise it is local.
 */
@Slf4j
@Singleton
public class ReferenceClassifier {

	private final GeneralProperties generalProperties;
	private final Set<String> localHosts;

	public ReferenceClassifier(ReferenceProperties referenceProperties, GeneralProperties generalProperties) {
		this.generalProperties = generalProperties;
		this.localHosts = normalize(referenceProperties.getLocalHosts());
	}

	/**
	 * A missing, relative or non-hierarchical href (e.g. the {@code urn:} id this deployment uses) counts as local.
	 */
	public ReferenceScope classify(ReferencedEntity ref) {
		URI href = ref.getReferenceHref();
		String host = href == null ? null : href.getHost();
		if (host == null || isLocalHost(host.toLowerCase(Locale.ROOT))) {
			return ReferenceScope.LOCAL;
		}
		return ReferenceScope.EXTERNAL;
	}

	// the server host is read per call, since it may be set after startup (see TestServerHostConfigurer)
	private boolean isLocalHost(String host) {
		return localHosts.contains(host) || host.equals(serverHost());
	}

	private String serverHost() {
		String serverHost = generalProperties.getServerHost();
		if (serverHost == null) {
			return null;
		}
		try {
			String host = URI.create(serverHost.trim()).getHost();
			return host == null ? null : host.toLowerCase(Locale.ROOT);
		} catch (IllegalArgumentException e) {
			log.warn("general.serverHost '{}' is not a valid URI; it is not used to classify references.",
					serverHost);
			return null;
		}
	}

	private static Set<String> normalize(List<String> hosts) {
		if (hosts == null) {
			return Set.of();
		}
		return hosts.stream()
				.filter(Objects::nonNull)
				.map(host -> host.trim().toLowerCase(Locale.ROOT))
				.filter(host -> !host.isEmpty())
				.collect(Collectors.toUnmodifiableSet());
	}
}
