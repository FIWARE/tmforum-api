package org.fiware.tmforum.migration.postgresql;

import org.fiware.ngsi.model.EntityVO;
import org.fiware.ngsi.model.GeoPropertyVO;
import org.fiware.ngsi.model.PropertyVO;
import org.fiware.tmforum.common.domain.subscription.TMForumSubscription;
import org.fiware.tmforum.customermanagement.domain.Customer;
import org.fiware.tmforum.migration.postgresql.RawAttributeCheck.Result;
import org.fiware.tmforum.party.domain.individual.Individual;
import org.fiware.tmforum.product.ProductOffering;
import org.fiware.tmforum.resourcefunction.domain.ResourceFunction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import tools.jackson.databind.BeanDescription;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.SerializationConfig;
import tools.jackson.databind.introspect.BeanPropertyDefinition;
import tools.jackson.databind.introspect.ClassIntrospector;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RawAttributeCheckTest {

	private static final String OFFERING = ProductOffering.TYPE_PRODUCT_OFFERING;
	private static final String HUB = TMForumSubscription.TYPE_TM_FORUM_SUBSCRIPTION;
	private static final URI ID = URI.create("urn:ngsi-ld:product-offering:1");

	static Stream<Arguments> attributeNames() {
		return Stream.of(
				Arguments.of(List.of("name", "description"), false, Set.of("name", "description")),
				Arguments.of(List.of("tmfEscaped-valueType", "name"), false, Set.of("valueType", "name")),
				Arguments.of(List.of("@context", "createdAt", "modifiedAt", "deletedAt", "name"), false, Set.of("name")),
				Arguments.of(List.of("name"), true, Set.of("name", EntityVO.JSON_PROPERTY_LOCATION)));
	}

	@ParameterizedTest
	@MethodSource("attributeNames")
	void takesUnescapedAttributeNamesWithoutSystemKeys(List<String> attributes, boolean withLocation,
			Set<String> expected) {
		EntityVO entity = new EntityVO().id(ID).type(OFFERING);
		attributes.forEach(attribute -> entity.setAdditionalProperties(attribute, new PropertyVO().value("v")));
		if (withLocation) {
			entity.setLocation(new GeoPropertyVO());
		}

		assertEquals(expected, RawAttributeCheck.attributeNames(entity));
	}

	static Stream<Arguments> checks() {
		return Stream.of(
				Arguments.of("all stored", OFFERING, ProductOffering.class, Set.of("name", "version"),
						Set.of("id", "type", "name", "version"), new Result(Set.of(), Set.of(), Set.of())),
				Arguments.of("dropped attribute", OFFERING, ProductOffering.class, Set.of("name", "description"),
						Set.of("id", "name"), new Result(Set.of("description"), Set.of(), Set.of())),
				Arguments.of("seeded version only in the target", OFFERING, ProductOffering.class, Set.of("name"),
						Set.of("id", "type", "name", "version"), new Result(Set.of(), Set.of(), Set.of("version"))),
				Arguments.of("dropped hub subscription", HUB, TMForumSubscription.class,
						Set.of("query", "subscription"), Set.of("id", "query"),
						new Result(Set.of(), Set.of("subscription"), Set.of())),
				Arguments.of("dropped subscription on a non-hub", OFFERING, ProductOffering.class,
						Set.of("subscription"), Set.of("id"), new Result(Set.of("subscription"), Set.of(), Set.of())),
				Arguments.of("renamed attribute stored", Individual.TYPE_INDIVIDUAL, Individual.class,
						Set.of("individualLocation"), Set.of("id", "location"), new Result(Set.of(), Set.of(), Set.of())),
				Arguments.of("renamed attribute dropped", Individual.TYPE_INDIVIDUAL, Individual.class,
						Set.of("individualLocation"), Set.of("id"),
						new Result(Set.of("individualLocation"), Set.of(), Set.of())),
				Arguments.of("unmapped extension stored under its name", OFFERING, ProductOffering.class,
						Set.of("extension"), Set.of("id", "extension"), new Result(Set.of(), Set.of(), Set.of())));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("checks")
	void reportsAttributesMissingInTheTarget(String description, String type, Class<?> domainClass,
			Set<String> sourceAttributes, Set<String> targetKeys, Result expected) {
		assertEquals(expected, new RawAttributeCheck(type, domainClass).check(sourceAttributes, targetKeys),
				description);
	}

	static Stream<Arguments> renamedAttributes() {
		return Stream.of(
				Arguments.of(Individual.class, "individualLocation", "location"),
				Arguments.of(Customer.class, "characteristics", "characteristic"),
				Arguments.of(ResourceFunction.class, "resourceGraph", "connectivity"));
	}

	@ParameterizedTest
	@MethodSource("renamedAttributes")
	void mapsRenamedAttributesToTheirDocumentKeys(Class<?> domainClass, String attribute, String key) {
		assertEquals(key, RawAttributeCheck.documentKeys(domainClass).get(attribute));
	}

	@Test
	void keepsTheTargetNameOfBridgeSetters() {
		Map<String, String> keys = RawAttributeCheck.documentKeys(TMForumSubscription.class);

		VerifyCommand.HUB_QUERY_ATTRIBUTES.forEach(attribute ->
				assertTrue(!keys.containsKey(attribute), attribute + " maps to " + keys.get(attribute)));
	}

	static Stream<Arguments> registryTypes() {
		EntityTypeRegistry registry = EntityTypeRegistry.scan();
		return registry.types().stream().map(type -> Arguments.of(type, registry.domainClass(type).orElseThrow()));
	}

	// guards the Lombok-based derivation: every derived key must be a property Jackson writes
	@ParameterizedTest(name = "{0}")
	@MethodSource("registryTypes")
	void derivesSerializedPropertyNames(String type, Class<?> domainClass) {
		Set<String> properties = serializedProperties(domainClass);

		RawAttributeCheck.documentKeys(domainClass).forEach((attribute, key) ->
				assertTrue(properties.contains(key), attribute + " maps to " + key + ", not a property of " + type));
	}

	private static Set<String> serializedProperties(Class<?> domainClass) {
		JsonMapper mapper = JsonMapper.builder().build();
		SerializationConfig config = mapper.serializationConfig();
		JavaType type = mapper.constructType(domainClass);
		ClassIntrospector introspector = config.classIntrospectorInstance();
		BeanDescription description =
				introspector.introspectForSerialization(type, introspector.introspectClassAnnotations(type));
		return description.findProperties().stream()
				.filter(BeanPropertyDefinition::couldSerialize)
				.map(BeanPropertyDefinition::getName)
				.collect(Collectors.toSet());
	}
}
