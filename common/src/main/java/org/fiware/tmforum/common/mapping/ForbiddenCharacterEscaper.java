package org.fiware.tmforum.common.mapping;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Escapes/unescapes string values so they never contain the NGSI-LD "Forbidden Characters"
 * ({@code < > " ' = ; ( )}, ETSI GS CIM 009 clause 4.6.4). Rejecting them is optional per spec,
 * but some brokers enforce it, while e.g. a TMForum subscription's raw/translated query filter
 * is inherently key=value-shaped and will always contain at least '='.
 * <p>
 * Controlled by {@link org.fiware.tmforum.common.configuration.GeneralProperties#isEscapeForbiddenCharacters()}
 * (see {@link ForbiddenCharacterEscapingConfigurer} for how that reaches this static flag) - on by
 * default; set to false for a broker (or existing data) that expects the plain, unescaped value.
 */
public final class ForbiddenCharacterEscaper {

	private static volatile boolean enabled = true;

	private ForbiddenCharacterEscaper() {
	}

	public static void setEnabled(boolean enabled) {
		ForbiddenCharacterEscaper.enabled = enabled;
	}

	public static String escape(String value) {
		return value == null || !enabled ? value : URLEncoder.encode(value, StandardCharsets.UTF_8);
	}

	public static String unescape(String value) {
		return value == null || !enabled ? value : URLDecoder.decode(value, StandardCharsets.UTF_8);
	}

}
