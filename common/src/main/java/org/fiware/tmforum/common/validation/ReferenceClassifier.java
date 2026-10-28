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
 * Classifies references as local or external, based on an operator-configured allow-list. A reference is external
 * when its href is absolute and names a host of {@code references.external-hosts} that is not one of this
 * deployment's own ({@code general.serverHost}, {@code references.local-hosts}); otherwise it is local. The href is
 * client-supplied, so an unconfigured deployment classifies everything as local and validates it.
 */
@Slf4j
@Singleton
public class ReferenceClassifier {

	private final GeneralProperties generalProperties;
	private final Set<String> localHosts;
	private final Set<String> externalHosts;
	// last seen general.serverHost and its parsed host, so a bad value is parsed and warned about once
	private volatile ParsedServerHost parsedServerHost;

	public ReferenceClassifier(ReferenceProperties referenceProperties, GeneralProperties generalProperties) {
		this.generalProperties = generalProperties;
		this.localHosts = normalize(referenceProperties.getLocalHosts());
		this.externalHosts = normalize(referenceProperties.getExternalHosts());
		logConfiguration();
	}

	/**
	 * A missing, relative or non-hierarchical href (e.g. the {@code urn:} id this deployment uses) counts as local,
	 * as does every host that is not configured as external.
	 */
	public ReferenceScope classify(ReferencedEntity ref) {
		URI href = ref.getReferenceHref();
		String host = href == null ? null : href.getHost();
		if (host == null) {
			return ReferenceScope.LOCAL;
		}
		String normalizedHost = host.toLowerCase(Locale.ROOT);
		if (isLocalHost(normalizedHost) || !externalHosts.contains(normalizedHost)) {
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
		ParsedServerHost parsed = parsedServerHost;
		if (parsed == null || !parsed.raw().equals(serverHost)) {
			parsed = new ParsedServerHost(serverHost, parseHost(serverHost));
			parsedServerHost = parsed;
		}
		return parsed.host();
	}

	private void logConfiguration() {
		if (externalHosts.isEmpty()) {
			log.info("No references.external-hosts are configured, so every reference is validated against local "
					+ "storage.");
			return;
		}
		log.info("References to the hosts {} are treated as external and are not validated.", externalHosts);
		Set<String> ownHosts = externalHosts.stream().filter(localHosts::contains).collect(Collectors.toSet());
		if (!ownHosts.isEmpty()) {
			log.warn("The hosts {} are configured as external and as local; they are treated as local.", ownHosts);
		}
	}

	private static String parseHost(String serverHost) {
		try {
			String host = URI.create(serverHost.trim()).getHost();
			if (host == null) {
				// e.g. a scheme-less "tmf.local:8632" parses with "tmf.local" as its scheme
				log.warn("general.serverHost '{}' has no host (missing scheme?); it is not used to classify "
						+ "references.", serverHost);
				return null;
			}
			return host.toLowerCase(Locale.ROOT);
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

	private record ParsedServerHost(String raw, String host) {
	}
}
