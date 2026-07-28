package org.fiware.tmforum.common.filter;

import io.micronaut.http.HttpHeaders;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.server.HttpServerConfiguration;
import io.micronaut.http.ssl.ServerSslConfiguration;
import jakarta.inject.Singleton;

import java.util.function.Consumer;

/**
 * Parses HTTP forwarding headers to extract client request information.
 *
 * <p>
 * This parser handles both the standard RFC 7239 `Forwarded` header and
 * configurable legacy `X-Forwarded-*` headers. It extracts key information
 * such as:
 * <ul>
 *     <li>Original client IP address (`for` / X-Forwarded-For)</li>
 *     <li>Original protocol (`proto` / X-Forwarded-Proto)</li>
 *     <li>Host and optional port (`host` / X-Forwarded-Host and X-Forwarded-Port)</li>
 *     <li>Proxy information (`by`)</li>
 *     <li>Path prefix added by upstream proxies (`X-Forwarded-Prefix`)</li>
 * </ul>
 * </p>
 */
@Singleton
public class ForwardHeaderParser {

	public static final int DEFAULT_HTTP_PORT = 80;
	public static final int DEFAULT_HTTPS_PORT = 443;

	private static final String DEFAULT_HOST = "localhost";
	private static final String HTTP_PROTOCOL = "http";
	private static final String HTTPS_PROTOCOL = "https";

	// RFC 7239 directive names
	private static final String FOR_DIRECTIVE = "for";
	private static final String HOST_DIRECTIVE = "host";
	private static final String PROTO_DIRECTIVE = "proto";
	private static final String BY_DIRECTIVE = "by";

	private final ForwardedForConfig config;
	private final int serverPort;
	private final String defaultServerProtocol;
	private final String defaultHost;

	public ForwardHeaderParser(ForwardedForConfig config, HttpServerConfiguration serverConfiguration,
							   ServerSslConfiguration sslConfig) {

		this.config = config;
		this.serverPort = serverConfiguration.getPort().orElse(HttpServerConfiguration.DEFAULT_PORT);
		this.defaultServerProtocol = sslConfig.isEnabled() ? HTTPS_PROTOCOL : HTTP_PROTOCOL;
		this.defaultHost = serverConfiguration.getHost().orElse(DEFAULT_HOST);
	}

	/**
	 * Parses the provided HTTP headers to extract forwarding information.
	 *
	 * <p>
	 * This method reads both the standard `Forwarded` header (RFC 7239) and
	 * any legacy `X-Forwarded-*` headers as configured in {@link ForwardedForConfig}.
	 * It populates a {@link ForwardedInfo} object with the extracted details:
	 * client IP (`for`), protocol (`proto`), host, port, proxy (`by`), and path prefix.
	 * `Forwarded` header values take precedence over legacy headers when both are present.
	 * </p>
	 *
	 * <p>
	 * The returned {@link ForwardedInfo} object can be used by downstream filters or
	 * controllers to reconstruct the original request URL or handle redirects correctly
	 * when behind reverse proxies.
	 * </p>
	 *
	 * @param request the incoming request
	 * @return a {@link ForwardedInfo} object containing the parsed forwarding details
	 */
	public ForwardedInfo parse(HttpRequest<?> request) {

		HttpHeaders headers = request.getHeaders();
		ForwardedInfo forwardedInfo = new ForwardedInfo(null, defaultServerProtocol, defaultHost, serverPort, null, "");
		parseLegacyForwardedHeaders(headers, forwardedInfo);
		parseForwardedHeader(headers, forwardedInfo);

		int port = forwardedInfo.getForwardedPort();
		String proto = forwardedInfo.getForwardedProto();

		if (port == 0) {
			forwardedInfo.setForwardedPort(HTTPS_PROTOCOL.equalsIgnoreCase(proto) ? DEFAULT_HTTPS_PORT : DEFAULT_HTTP_PORT);
		} else if (port == -1) {
			// Use the actual server port if the forwarded port is invalid
			forwardedInfo.setForwardedPort(request.getServerAddress().getPort());
		}

		return forwardedInfo;
	}

	private void parseForwardedHeader(HttpHeaders headers, ForwardedInfo defaultEntry) {

		String forwardedHeader = headers.get(HttpHeaders.FORWARDED);
		if (forwardedHeader == null || forwardedHeader.isEmpty()) {
			return ;
		}

		String[] directives = forwardedHeader.split(";");

		for (String directive : directives) {
			// get only first keyValue if multiple are present
			String[] keyValue = directive.trim().split(",")[0].split("=", 2);
			if (keyValue.length == 2) {
				String key = keyValue[0].trim().toLowerCase();
				String value = keyValue[1].trim().replaceAll("^\"|\"$", ""); // remove quotes
				switch (key) {
					case FOR_DIRECTIVE:
						defaultEntry.setForwardedFor(value);
						break;
					case PROTO_DIRECTIVE:
						defaultEntry.setForwardedProto(value);
						break;
					case HOST_DIRECTIVE:
						// Extract host and optional port
						if (value.contains(":")) {
							String[] hostParts = value.split(":", 2);
							defaultEntry.setForwardedHost(hostParts[0]);
							try {
								defaultEntry.setForwardedPort(Integer.parseInt(hostParts[1]));
							} catch (NumberFormatException e) {
								defaultEntry.setForwardedPort(0);
							}
						} else {
							defaultEntry.setForwardedHost(value);
							defaultEntry.setForwardedPort(0);
						}
						break;
					case BY_DIRECTIVE:
						defaultEntry.setForwardedBy(value);
						break;
				}
			}
		}
	}

	private void parseLegacyForwardedHeaders(HttpHeaders headers, ForwardedInfo defaultEntry) {

		getHeaderValue(headers, config.getForHeader(), defaultEntry::setForwardedFor);
		getHeaderValue(headers, config.getProtocolHeader(), defaultEntry::setForwardedProto);
		getHeaderValue(headers, config.getHostHeader(), defaultEntry::setForwardedHost);
		getHeaderValue(headers, config.getPortHeader(), defaultEntry::setForwardedPortStr);
		getHeaderValue(headers, config.getPrefixHeader(), defaultEntry::setForwardedPrefix);
	}

	private void getHeaderValue(HttpHeaders headers, String headerName, Consumer<String> setter) {

		if (headerName != null) {
			String value = headers.get(headerName);
			if (value != null) {
				setter.accept(value);
			}
		}
	}
}
