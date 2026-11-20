package org.fiware.tmforum.migration.postgresql;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.ApplicationContextBuilder;
import org.fiware.tmforum.common.CommonConstants;
import org.fiware.tmforum.common.mapping.ForbiddenCharacterEscaper;
import picocli.CommandLine.Option;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The broker-backed source and the PostgreSQL target {@link ApplicationContext}, configured from the CLI options.
 * Both contexts get the same {@code general.escape-forbidden-characters}, since every creation of
 * {@code GeneralProperties}, in either context, rewrites the JVM-wide {@link ForbiddenCharacterEscaper} flag.
 */
public class MigrationContexts implements AutoCloseable {

	static final String IN_MEMORY_ENVIRONMENT = "in-memory";

	static final String NGSI_URL_PROPERTY = "micronaut.http.services.ngsi.url";
	static final String TENANT_PROPERTY = "general.tenant";
	static final String ESCAPE_PROPERTY = "general.escape-forbidden-characters";
	static final String SERIALIZATION_INCLUSION_PROPERTY = "jackson.serialization-inclusion";
	// Micronaut's NON_EMPTY default would drop empty values and lists of migrated entities
	static final String SERIALIZATION_INCLUSION = "non_null";
	static final String WRITE_DATES_AS_TIMESTAMPS_PROPERTY = "jackson.date-time-features.write-dates-as-timestamps";
	static final String STRICT_RELATIONSHIPS_PROPERTY = "mapping.strictRelationships";
	// all-in-one's application.yaml leaves it to an env var without a default
	static final String API_EXTENSION_PROPERTY = "apiExtension.enabled";
	static final String OUTBOX_ENABLED_PROPERTY = "tmforum.outbox.enabled";
	static final String R2DBC_URL_PROPERTY = "r2dbc.datasources.default.url";
	static final String R2DBC_USER_PROPERTY = "r2dbc.datasources.default.username";
	static final String R2DBC_PASSWORD_PROPERTY = "r2dbc.datasources.default.password";
	static final String FLYWAY_ENABLED_PROPERTY = "flyway.datasources.default.enabled";
	static final String FLYWAY_URL_PROPERTY = "flyway.datasources.default.url";
	static final String FLYWAY_USER_PROPERTY = "flyway.datasources.default.user";
	static final String FLYWAY_PASSWORD_PROPERTY = "flyway.datasources.default.password";
	private static final String R2DBC_URL_TEMPLATE = "r2dbc:pool:postgresql://%s:%d/%s";
	private static final String JDBC_URL_TEMPLATE = "jdbc:postgresql://%s:%d/%s";

	@Option(names = "--broker-url", description = "Base URL of the source NGSI-LD broker.",
			defaultValue = "http://localhost:1026")
	String brokerUrl;

	@Option(names = "--broker-type", description = "Source broker: ${COMPLETION-CANDIDATES}.",
			defaultValue = "orion-ld", converter = BrokerType.Converter.class, completionCandidates = BrokerType.Names.class)
	BrokerType brokerType;

	@Option(names = "--tenant", description = "NGSI-LD tenant of the source broker; none by default.")
	String tenant;

	@Option(names = "--source-escaped", arity = "1", defaultValue = "true",
			description = "Whether the broker data was written with general.escape-forbidden-characters=true.")
	boolean sourceEscaped;

	@Option(names = "--pg-host", defaultValue = "${env:POSTGRES_HOST:-localhost}")
	String pgHost;

	@Option(names = "--pg-port", defaultValue = "${env:POSTGRES_PORT:-5432}")
	int pgPort;

	@Option(names = "--pg-db", defaultValue = "${env:POSTGRES_DB:-tmforum}")
	String pgDb;

	@Option(names = "--pg-user", defaultValue = "${env:POSTGRES_USER:-tmforum}")
	String pgUser;

	@Option(names = "--pg-password", defaultValue = "${env:POSTGRES_PASSWORD:-tmforum}")
	String pgPassword;

	private ApplicationContext source;
	private ApplicationContext target;

	/**
	 * Starts both contexts; the target runs the Flyway migrations, so it needs the database.
	 */
	public void start() {
		start(Map.of());
	}

	// targetOverrides lets tests start the target without a database
	void start(Map<String, Object> targetOverrides) {
		source = builder(sourceEnvironments(), sourceProperties()).start();
		Map<String, Object> targetProperties = new HashMap<>(targetProperties());
		targetProperties.putAll(targetOverrides);
		target = builder(targetEnvironments(), targetProperties).start();
	}

	/**
	 * The started source context. Throws {@link IllegalStateException} before {@link #start()}.
	 */
	public ApplicationContext source() {
		return requireStarted(source);
	}

	/**
	 * The started target context. Throws {@link IllegalStateException} before {@link #start()}.
	 */
	public ApplicationContext target() {
		return requireStarted(target);
	}

	/**
	 * Fails when the escaper flag no longer matches {@code --source-escaped}, i.e. data was read with the wrong
	 * decoding. Every command calls it when it finishes.
	 */
	public void verifyEscaperFlag() {
		if (ForbiddenCharacterEscaper.isEnabled() != sourceEscaped) {
			throw new IllegalStateException(String.format(
					"The forbidden-character escaper is %s, but --source-escaped is %s; the run is not trustworthy.",
					ForbiddenCharacterEscaper.isEnabled(), sourceEscaped));
		}
	}

	@Override
	public void close() {
		closeQuietly(target);
		closeQuietly(source);
		target = null;
		source = null;
	}

	List<String> sourceEnvironments() {
		return List.of(brokerType.getEnvironment(), IN_MEMORY_ENVIRONMENT);
	}

	List<String> targetEnvironments() {
		return List.of(IN_MEMORY_ENVIRONMENT, CommonConstants.POSTGRESQL_ENVIRONMENT);
	}

	Map<String, Object> sourceProperties() {
		Map<String, Object> properties = sharedProperties();
		properties.put(NGSI_URL_PROPERTY, brokerUrl);
		if (tenant != null) {
			properties.put(TENANT_PROPERTY, tenant);
		}
		return properties;
	}

	Map<String, Object> targetProperties() {
		Map<String, Object> properties = sharedProperties();
		// a bulk import must not notify the hubs, see PostgresEntityRepository#importDomainEntity
		properties.put(OUTBOX_ENABLED_PROPERTY, false);
		properties.put(R2DBC_URL_PROPERTY, String.format(R2DBC_URL_TEMPLATE, pgHost, pgPort, pgDb));
		properties.put(R2DBC_USER_PROPERTY, pgUser);
		properties.put(R2DBC_PASSWORD_PROPERTY, pgPassword);
		properties.put(FLYWAY_ENABLED_PROPERTY, true);
		properties.put(FLYWAY_URL_PROPERTY, String.format(JDBC_URL_TEMPLATE, pgHost, pgPort, pgDb));
		properties.put(FLYWAY_USER_PROPERTY, pgUser);
		properties.put(FLYWAY_PASSWORD_PROPERTY, pgPassword);
		return properties;
	}

	private Map<String, Object> sharedProperties() {
		Map<String, Object> properties = new HashMap<>();
		properties.put(SERIALIZATION_INCLUSION_PROPERTY, SERIALIZATION_INCLUSION);
		properties.put(WRITE_DATES_AS_TIMESTAMPS_PROPERTY, false);
		properties.put(STRICT_RELATIONSHIPS_PROPERTY, false);
		properties.put(API_EXTENSION_PROPERTY, false);
		properties.put(ESCAPE_PROPERTY, sourceEscaped);
		return properties;
	}

	// no deduction, so a cloud or test environment cannot pull in other configuration
	private static ApplicationContextBuilder builder(List<String> environments, Map<String, Object> properties) {
		return ApplicationContext.builder()
				.deduceEnvironment(false)
				.environments(environments.toArray(String[]::new))
				.properties(properties);
	}

	private static ApplicationContext requireStarted(ApplicationContext context) {
		if (context == null) {
			throw new IllegalStateException("The migration contexts are not started.");
		}
		return context;
	}

	private static void closeQuietly(ApplicationContext context) {
		if (context != null && context.isRunning()) {
			context.close();
		}
	}
}
