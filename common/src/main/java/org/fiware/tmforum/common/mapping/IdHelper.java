package org.fiware.tmforum.common.mapping;


import org.mapstruct.Context;
import org.mapstruct.Named;

import java.net.URI;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

import static org.fiware.tmforum.common.CommonTemplates.ID_TEMPLATE;

@Named("IdHelper")
public class IdHelper {

	private final static Pattern NGSI_LD_URN_PATTERN = Pattern.compile(
			"^urn:ngsi-ld:[a-z0-9-]{0,31}:([a-z0-9()+,\\-.:=@;$_!*']|%[0-9a-f]{2})+$",
			Pattern.CASE_INSENSITIVE);

	/**
	 * Check if the given id is NGSI-LD compliant.
	 *
	 * @param id the id to check
	 * @return true if its NGSI-LD compliant
	 */
	public static boolean isNgsiLdId(String id) {
		return Optional.ofNullable(id)
				.filter(nonNullId -> NGSI_LD_URN_PATTERN.matcher(nonNullId).matches())
				.isPresent();
	}

	/**
	 * Create an NGSI-LD compliant id
	 *
	 * @param id         id to be translated
	 * @param targetType entity type
	 * @return the compliant id
	 */
	@Named("ToNgsiLd")
	public static URI toNgsiLd(String id, @Context String targetType) {
		return URI.create(String.format(ID_TEMPLATE, targetType, Optional.ofNullable(id).orElse(UUID.randomUUID().toString())));
	}

	/**
	 * Extract the id part from the NGSI-LD id
	 *
	 * @param id the full id
	 * @return only the id part
	 */
	@Named("FromNgsiLd")
	public static String fromNgsiLd(URI id) {
		return Optional.ofNullable(id)
				.map(URI::toString)
				.filter(IdHelper::isNgsiLdId)
				.map(stringId -> id.toString().split(":"))
				.map(stringArray -> stringArray[stringArray.length - 1])
				.orElseThrow(() -> new RuntimeException(String.format("Did not receive a valid uri: %s", id)));
	}

}
