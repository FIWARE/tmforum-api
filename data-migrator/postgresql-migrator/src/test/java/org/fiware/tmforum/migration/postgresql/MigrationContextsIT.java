package org.fiware.tmforum.migration.postgresql;

import org.fiware.tmforum.common.mapping.ForbiddenCharacterEscaper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Starts both contexts against the test cluster's PostgreSQL (k3s/postgresql-db, the {@code POSTGRES_*} defaults).
 */
class MigrationContextsIT {

	private static final String JDBC_URL = "jdbc:postgresql://localhost:5432/tmforum";
	private static final String DB_CREDENTIAL = "tmforum";
	private static final String ENTITY_TABLE_QUERY = "SELECT to_regclass('tmforum.entity') IS NOT NULL";

	private final MigrationContexts contexts = new MigrationContexts();

	@AfterEach
	void close() {
		contexts.close();
		ForbiddenCharacterEscaper.setEnabled(true);
	}

	@Test
	void startsBothContextsAndMigratesTheSchema() throws SQLException {
		new CommandLine(contexts).parseArgs();
		contexts.start();

		assertEquals(Set.copyOf(contexts.sourceEnvironments()), MigrationContextsTest.ownEnvironments(contexts.source()));
		assertEquals(Set.copyOf(contexts.targetEnvironments()), MigrationContextsTest.ownEnvironments(contexts.target()));
		try (Connection connection = DriverManager.getConnection(JDBC_URL, DB_CREDENTIAL, DB_CREDENTIAL);
			 Statement statement = connection.createStatement();
			 ResultSet result = statement.executeQuery(ENTITY_TABLE_QUERY)) {
			assertTrue(result.next() && result.getBoolean(1), "Flyway did not create tmforum.entity");
		}
		contexts.verifyEscaperFlag();
	}
}
