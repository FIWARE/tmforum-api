package org.fiware.tmforum.common.mapping;


import io.github.wistefan.mapping.annotations.MappingEnabled;
import org.mapstruct.Context;
import org.mapstruct.Named;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

import static org.fiware.tmforum.common.CommonConstants.ID_TEMPLATE;

@Named("IdHelper")
public class IdHelper {

	private final static Pattern NGSI_LD_URN_PATTERN = Pattern.compile(
			"^urn:ngsi-ld:[a-z0-9-]{1,63}:([a-z0-9()+,\\-.:=@;$_!*']|%[0-9a-f]{2})+$",
			Pattern.CASE_INSENSITIVE);
	// urn:ngsi-ld:<type>
	private static final int ID_PARTS_UP_TO_TYPE = 3;
	private static final int TYPE_SEGMENT_INDEX = 2;

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
	 * Whether {@code id} has a type segment ({@code urn:ngsi-ld:<type>:...}) that is one of the class's
	 * MappingEnabled entity types; the storage adapters read only such ids.
	 *
	 * @throws IllegalArgumentException if the id has a type segment and the class has no MappingEnabled annotation
	 */
	public static boolean hasTypeSegmentOf(URI id, Class<?> entityClass) {
		String[] parts = id.toString().split(":", ID_PARTS_UP_TO_TYPE + 1);
		if (parts.length < ID_PARTS_UP_TO_TYPE) {
			return false;
		}
		MappingEnabled mappingAnnotation = entityClass.getAnnotation(MappingEnabled.class);
		if (mappingAnnotation == null) {
			throw new IllegalArgumentException(
					String.format("Class %s missing @MappingEnabled annotation", entityClass.getName()));
		}
		return Arrays.asList(mappingAnnotation.entityType()).contains(parts[TYPE_SEGMENT_INDEX]);
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

	@Named("NullableListMapper")
	public static <T> List<T> nullableListMapper(List<T> theNullableList) {
		return theNullableList;
	}
}
