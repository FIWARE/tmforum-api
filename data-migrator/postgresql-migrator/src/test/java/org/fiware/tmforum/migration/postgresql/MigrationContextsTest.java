package org.fiware.tmforum.migration.postgresql;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.env.Environment;
import org.fiware.tmforum.common.CommonConstants;
import org.fiware.tmforum.common.configuration.GeneralProperties;
import org.fiware.tmforum.common.configuration.NgsiLdBrokerProperties;
import org.fiware.tmforum.common.mapping.ForbiddenCharacterEscaper;
import org.fiware.tmforum.common.validation.ReferenceClassifier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import picocli.CommandLine;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MigrationContextsTest {

	private static final String BROKER_URL = "http://broker:1026";
	private static final String TENANT = "tenant-a";
	private static final String PG_HOST = "pg";
	private static final String PG_PORT = "5433";
	private static final String PG_DB = "db";
	private static final String PG_USER = "user";
	private static final String PG_PASSWORD = "secret";
	// the tests start the target without a database
	private static final Map<String, Object> NO_DATABASE = Map.of(MigrationContexts.FLYWAY_ENABLED_PROPERTY, false);

	// Micronaut adds these from KUBERNETES_SERVICE_HOST regardless of the builder's deduction switches
	private static final Set<String> PLATFORM_ENVIRONMENTS = Set.of(Environment.KUBERNETES, Environment.CLOUD);

	private MigrationContexts contexts;

	// the context's active environments, without the platform names Micronaut adds on Kubernetes
	static Set<String> ownEnvironments(ApplicationContext context) {
		Set<String> names = new HashSet<>(context.getEnvironment().getActiveNames());
		names.removeAll(PLATFORM_ENVIRONMENTS);
		return names;
	}

	@AfterEach
	void closeAndResetEscaper() {
		if (contexts != null) {
			contexts.close();
		}
		ForbiddenCharacterEscaper.setEnabled(true);
		System.clearProperty(Environment.ENVIRONMENTS_PROPERTY);
	}

	private static MigrationContexts parse(String... args) {
		MigrationContexts parsed = new MigrationContexts();
		new CommandLine(parsed).parseArgs(args);
		return parsed;
	}

	@ParameterizedTest
	@EnumSource(BrokerType.class)
	void buildsTheSourceConfiguration(BrokerType brokerType) {
		contexts = parse("--broker-type", brokerType.getEnvironment(), "--broker-url", BROKER_URL,
				"--tenant", TENANT);

		assertEquals(List.of(brokerType.getEnvironment(), MigrationContexts.IN_MEMORY_ENVIRONMENT),
				contexts.sourceEnvironments());
		Map<String, Object> properties = contexts.sourceProperties();
		assertEquals(BROKER_URL, properties.get(MigrationContexts.NGSI_URL_PROPERTY));
		assertEquals(TENANT, properties.get(MigrationContexts.TENANT_PROPERTY));
		assertEquals(false, properties.get(MigrationContexts.REPLACE_ON_UPDATE_PROPERTY));
		assertSharedProperties(properties, true);
	}

	@ParameterizedTest
	@CsvSource(value = {"orion-ld;;false", "orion-ld;false;false", "scorpio;;false", "scorpio;false;false",
			"scorpio;true;true"}, delimiter = ';')
	void passesReplaceOnUpdateToTheSource(String brokerType, String replaceOnUpdate, boolean expected) {
		contexts = replaceOnUpdate == null
				? parse("--broker-type", brokerType)
				: parse("--broker-type", brokerType, "--replace-on-update", replaceOnUpdate);

		assertEquals(expected, contexts.sourceProperties().get(MigrationContexts.REPLACE_ON_UPDATE_PROPERTY));
	}

	@ParameterizedTest
	@CsvSource(value = {"orion-ld;;", "orion-ld;false;", "orion-ld;true;not supported on Orion-LD",
			"scorpio;;Set --replace-on-update for Scorpio", "scorpio;false;", "scorpio;true;"}, delimiter = ';')
	void checksTheBrokerWriteMode(String brokerType, String replaceOnUpdate, String expectedProblem) {
		contexts = replaceOnUpdate == null
				? parse("--broker-type", brokerType)
				: parse("--broker-type", brokerType, "--replace-on-update", replaceOnUpdate);

		Optional<String> problem = contexts.brokerWriteModeProblem();

		assertEquals(expectedProblem != null, problem.isPresent(), problem.toString());
		problem.ifPresent(message -> assertTrue(message.contains(expectedProblem), message));
	}

	@Test
	void buildsTheTargetConfiguration() {
		contexts = parse("--pg-host", PG_HOST, "--pg-port", PG_PORT, "--pg-db", PG_DB, "--pg-user", PG_USER,
				"--pg-password", PG_PASSWORD);

		assertEquals(List.of(MigrationContexts.IN_MEMORY_ENVIRONMENT, CommonConstants.POSTGRESQL_ENVIRONMENT),
				contexts.targetEnvironments());
		Map<String, Object> properties = contexts.targetProperties();
		assertEquals(false, properties.get(MigrationContexts.OUTBOX_ENABLED_PROPERTY));
		assertEquals("r2dbc:pool:postgresql://pg:5433/db", properties.get(MigrationContexts.R2DBC_URL_PROPERTY));
		assertEquals("jdbc:postgresql://pg:5433/db", properties.get(MigrationContexts.FLYWAY_URL_PROPERTY));
		assertEquals(true, properties.get(MigrationContexts.FLYWAY_ENABLED_PROPERTY));
		assertEquals(PG_USER, properties.get(MigrationContexts.R2DBC_USER_PROPERTY));
		assertEquals(PG_PASSWORD, properties.get(MigrationContexts.R2DBC_PASSWORD_PROPERTY));
		assertEquals(PG_USER, properties.get(MigrationContexts.FLYWAY_USER_PROPERTY));
		assertEquals(PG_PASSWORD, properties.get(MigrationContexts.FLYWAY_PASSWORD_PROPERTY));
		assertFalse(properties.containsKey(MigrationContexts.NGSI_URL_PROPERTY));
		assertSharedProperties(properties, true);
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void passesSourceEscapedToBothContexts(boolean sourceEscaped) {
		contexts = parse("--source-escaped", String.valueOf(sourceEscaped));

		assertSharedProperties(contexts.sourceProperties(), sourceEscaped);
		assertSharedProperties(contexts.targetProperties(), sourceEscaped);
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void escaperFlagSurvivesLateGeneralPropertiesCreation(boolean sourceEscaped) {
		contexts = parse("--source-escaped", String.valueOf(sourceEscaped));
		contexts.start(NO_DATABASE);

		for (ApplicationContext context : List.of(contexts.source(), contexts.target())) {
			assertEquals(sourceEscaped, context.getBean(GeneralProperties.class).isEscapeForbiddenCharacters());
			assertEquals(sourceEscaped, ForbiddenCharacterEscaper.isEnabled());
			context.getBean(ReferenceClassifier.class);
			assertEquals(sourceEscaped, ForbiddenCharacterEscaper.isEnabled());
		}
		contexts.verifyEscaperFlag();
	}

	@ParameterizedTest
	@EnumSource(BrokerType.class)
	void contextsRunOnlyTheirOwnEnvironments(BrokerType brokerType) {
		contexts = parse("--broker-type", brokerType.getEnvironment());
		contexts.start(NO_DATABASE);

		assertEquals(Set.copyOf(contexts.sourceEnvironments()), ownEnvironments(contexts.source()));
		assertEquals(Set.copyOf(contexts.targetEnvironments()), ownEnvironments(contexts.target()));
		assertFalse(contexts.source().getBean(NgsiLdBrokerProperties.class).isReplaceOnUpdate());
	}

	@ParameterizedTest
	@CsvSource(value = {"in-memory,orion-ld;", ";postgresql", "scorpio;in-memory"}, delimiter = ';')
	void startRejectsInheritedEnvironments(String systemProperty, String environmentVariable) {
		if (systemProperty != null) {
			System.setProperty(Environment.ENVIRONMENTS_PROPERTY, systemProperty);
		}
		contexts = parse();
		contexts.environmentVariables = name -> Environment.ENVIRONMENTS_ENV.equals(name) ? environmentVariable : null;

		assertThrows(IllegalStateException.class, () -> contexts.start(NO_DATABASE));
		assertThrows(IllegalStateException.class, contexts::source);
		assertThrows(IllegalStateException.class, contexts::target);
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void verifyEscaperFlagFailsOnMismatch(boolean sourceEscaped) {
		contexts = parse("--source-escaped", String.valueOf(sourceEscaped));
		ForbiddenCharacterEscaper.setEnabled(!sourceEscaped);

		assertThrows(IllegalStateException.class, contexts::verifyEscaperFlag);
	}

	@Test
	void contextsAreUnavailableBeforeStart() {
		contexts = parse();

		assertThrows(IllegalStateException.class, contexts::source);
		assertThrows(IllegalStateException.class, contexts::target);
	}

	private static void assertSharedProperties(Map<String, Object> properties, boolean sourceEscaped) {
		assertEquals(MigrationContexts.SERIALIZATION_INCLUSION,
				properties.get(MigrationContexts.SERIALIZATION_INCLUSION_PROPERTY));
		assertEquals(false, properties.get(MigrationContexts.WRITE_DATES_AS_TIMESTAMPS_PROPERTY));
		assertEquals(false, properties.get(MigrationContexts.STRICT_RELATIONSHIPS_PROPERTY));
		assertEquals(sourceEscaped, properties.get(MigrationContexts.ESCAPE_PROPERTY));
	}
}
