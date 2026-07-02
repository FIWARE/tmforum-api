package org.fiware.tmforum.common.filter;

import io.micronaut.context.annotation.ConfigurationInject;
import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.core.annotation.Nullable;
import lombok.Getter;

/**
 * Configuration for processing forwarded request headers.
 * This configuration is used to extract information from headers such as
 * Forwarded and X-Forwarded-* to determine the original request details.
 * `Forwarded` header, as defined in RFC 7239, is preferred over `X-Forwarded-*` headers.
 *
 * <p>
 * Unlike a single-deployment application, this connector ships as 18 independently deployed
 * modules, each with its own {@code application.yaml}. Requiring every module to configure these
 * header names explicitly would be needless duplication, so each defaults to the standard
 * de-facto header name and only needs {@code micronaut.server.forward-headers.*} set if a
 * specific deployment uses non-standard header names.
 * </p>
 */
@ConfigurationProperties("micronaut.server.forward-headers")
@Getter
public class ForwardedForConfig {

	private static final String DEFAULT_PROTOCOL_HEADER = "X-Forwarded-Proto";
	private static final String DEFAULT_PORT_HEADER = "X-Forwarded-Port";
	private static final String DEFAULT_HOST_HEADER = "X-Forwarded-Host";
	private static final String DEFAULT_PREFIX_HEADER = "X-Forwarded-Prefix";
	private static final String DEFAULT_FOR_HEADER = "X-Forwarded-For";

	/**
	 * The name of the header that carries the protocol.
	 * Default: "X-Forwarded-Proto".
	 */
	private String protocolHeader;

	/**
	 * The name of the header that carries the port.
	 * Default: "X-Forwarded-Port".
	 */
	private String portHeader;

	/**
	 * The name of the header that carries the host.
	 * Default: "X-Forwarded-Host".
	 */
	private String hostHeader;

	/**
	 * The name of the header that carries the context path or prefix of the request.
	 * Default: "X-Forwarded-Prefix".
	 */
	private String prefixHeader;

	/**
	 * The name of the header that carries the client's IP address.
	 * Default: "X-Forwarded-For".
	 */
	private String forHeader;

	@ConfigurationInject
	public ForwardedForConfig(
			@Nullable String protocolHeader,
			@Nullable String portHeader,
			@Nullable String hostHeader,
			@Nullable String prefixHeader,
			@Nullable String forHeader) {

		this.protocolHeader = protocolHeader != null ? protocolHeader : DEFAULT_PROTOCOL_HEADER;
		this.portHeader = portHeader != null ? portHeader : DEFAULT_PORT_HEADER;
		this.hostHeader = hostHeader != null ? hostHeader : DEFAULT_HOST_HEADER;
		this.prefixHeader = prefixHeader != null ? prefixHeader : DEFAULT_PREFIX_HEADER;
		this.forHeader = forHeader != null ? forHeader : DEFAULT_FOR_HEADER;
	}
}
