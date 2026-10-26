package org.fiware.tmforum.common.mapping;

import lombok.Data;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Pins the wire format of {@link FieldCleaningSerializer}, which does not see the jackson.* config.
 */
class FieldCleaningSerializerTest {

	private static final String MODULE_NAME = "FieldCleaningSerializerTest";

	private final ObjectMapper objectMapper = JsonMapper.builder()
			.addModule(new SimpleModule(MODULE_NAME).addSerializer(OrderedVO.class, new FieldCleaningSerializer<>()))
			.build();

	@ParameterizedTest
	@CsvSource({
			"urn:a, z, a, 1970-01-01T00:00:00Z",
			"urn:b, zeta, alpha, 2026-10-26T14:28:12.945Z"})
	void keepsDeclarationOrderAndIsoDates(String id, String zeta, String alpha, String date) {
		OrderedVO vo = new OrderedVO();
		vo.setId(id);
		vo.setZeta(zeta);
		vo.setAlpha(alpha);
		vo.setDate(Instant.parse(date));

		String json = objectMapper.writeValueAsString(vo);

		assertEquals(String.format("{\"id\":\"%s\",\"zeta\":\"%s\",\"alpha\":\"%s\",\"date\":\"%s\"}",
				id, zeta, alpha, date), json, "Fields should keep declaration order and dates should be ISO strings.");
	}

	/**
	 * Mutable bean like the generated VOs (a creator would fix the order anyway); declared out of alphabetical order.
	 */
	@Data
	public static class OrderedVO {
		private String id;
		private String zeta;
		private String alpha;
		private Instant date;
	}
}
