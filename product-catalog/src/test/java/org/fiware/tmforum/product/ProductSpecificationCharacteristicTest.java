package org.fiware.tmforum.product;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@code atValueType} carries the NGSI-LD attribute name for this TMForum characteristic's
 * {@code valueType} field - Coraine rejects a Property literally named {@code valueType}. The
 * {@link com.fasterxml.jackson.annotation.JsonAlias} on it has to keep reading both the new key and
 * entities written under the old {@code valueType} key before this fix.
 */
class ProductSpecificationCharacteristicTest {

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	public void deserializesTheNewKey() throws Exception {
		ProductSpecificationCharacteristic characteristic = objectMapper.readValue(
				"{\"atValueType\":\"string\"}", ProductSpecificationCharacteristic.class);

		assertEquals("string", characteristic.getAtValueType());
	}

	@Test
	public void deserializesTheLegacyKeyForBackwardCompatibility() throws Exception {
		ProductSpecificationCharacteristic characteristic = objectMapper.readValue(
				"{\"valueType\":\"string\"}", ProductSpecificationCharacteristic.class);

		assertEquals("string", characteristic.getAtValueType());
	}

	@Test
	public void serializationAlwaysUsesTheNewKey() throws Exception {
		ProductSpecificationCharacteristic characteristic = new ProductSpecificationCharacteristic();
		characteristic.setAtValueType("string");

		String json = objectMapper.writeValueAsString(characteristic);

		assertTrue(json.contains("\"atValueType\":\"string\""));
		assertFalse(json.contains("\"valueType\":\"string\""));
	}
}
