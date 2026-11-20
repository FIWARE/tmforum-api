package org.fiware.tmforum.migration.postgresql;

import io.r2dbc.spi.ConnectionFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Spec;

import java.util.List;
import java.util.Set;

/**
 * Moves a broker-backed deployment onto PostgreSQL and backfills inline references on either backend.
 * Subcommands not implemented yet are stubs that exit non-zero.
 */
@Command(name = "postgresql-migrator", mixinStandardHelpOptions = true,
		description = "Migrates TMForum data from an NGSI-LD broker to PostgreSQL.")
public class Application implements Runnable {

	private static final String DRY_RUN_DESCRIPTION = "Report what would change without writing.";
	private static final String DEFAULT_PAGE_SIZE = "100";
	private static final String TARGET_POSTGRESQL = "postgresql";
	private static final String TARGET_BROKER = "broker";
	private static final String NOT_IMPLEMENTED = "The '%s' command is not implemented yet.";

	@Spec
	CommandSpec spec;

	public static void main(String[] args) {
		System.exit(new CommandLine(new Application()).execute(args));
	}

	@Override
	public void run() {
		throw new ParameterException(spec.commandLine(), "Missing command.");
	}

	@Command(name = "import", description = "Copy every entity from the broker into PostgreSQL, without events.")
	int importEntities(@Mixin MigrationContexts contexts,
			@Option(names = "--dry-run", description = DRY_RUN_DESCRIPTION) boolean dryRun,
			@Option(names = "--types", split = ",", description = "Entity types to import; all mapped types by default.")
			List<String> types,
			@Option(names = "--page-size", defaultValue = DEFAULT_PAGE_SIZE,
					description = "Entities per broker request; Orion-LD caps it at 1000.") int pageSize,
			@Option(names = "--resume", description = "Continue an earlier import: accept a non-empty target and skip "
					+ "ids that are already stored.") boolean resume,
			@Option(names = "--fail-fast", description = "Stop at the first failure.") boolean failFast,
			@Option(names = "--backfill-references", description = "After a successful import, run "
					+ "backfill-references --target " + TARGET_POSTGRESQL + ".") boolean backfillReferences) {
		ImportOptions options;
		Set<String> resolvedTypes;
		EntityTypeRegistry registry = EntityTypeRegistry.scan();
		try {
			options = new ImportOptions(pageSize, resume, failFast, dryRun);
			resolvedTypes = ImportCommand.resolveTypes(registry, types);
		} catch (IllegalArgumentException e) {
			throw new ParameterException(spec.commandLine(), e.getMessage());
		}
		try (contexts) {
			contexts.start();
			ImportReport report = new ImportCommand(new BrokerSource(contexts.source()),
					new PostgresTarget(contexts.target()), registry, options).run(resolvedTypes);
			try {
				contexts.verifyEscaperFlag();
			} catch (IllegalStateException e) {
				report.escaperDrift(e.getMessage());
			}
			report.print(spec.commandLine().getOut());
			return report.succeeded() ? ExitCode.OK : ExitCode.SOFTWARE;
		}
	}

	@Command(name = "backfill-references", description = "Fill absent fields of stored references from their targets.")
	int backfillReferences(@Mixin MigrationContexts contexts,
			@Option(names = "--target", required = true,
					description = "Store to backfill: " + TARGET_POSTGRESQL + " or " + TARGET_BROKER + ".") String target,
			@Option(names = "--dry-run", description = DRY_RUN_DESCRIPTION) boolean dryRun) {
		if (TARGET_BROKER.equals(target)) {
			return notImplemented("backfill-references --target " + TARGET_BROKER);
		}
		if (!TARGET_POSTGRESQL.equals(target)) {
			throw new ParameterException(spec.commandLine(), String.format(
					"Unknown --target '%s', expected %s or %s.", target, TARGET_POSTGRESQL, TARGET_BROKER));
		}
		try (contexts) {
			contexts.startTarget();
			ReferenceBackfillReport report = postgresBackfill(contexts).run(dryRun);
			try {
				contexts.verifyEscaperFlag();
			} catch (IllegalStateException e) {
				report.escaperDrift(e.getMessage());
			}
			report.print(spec.commandLine().getOut());
			return report.succeeded() ? ExitCode.OK : ExitCode.SOFTWARE;
		}
	}

	private static PostgresReferenceBackfill postgresBackfill(MigrationContexts contexts) {
		return new PostgresReferenceBackfill(contexts.target().getBean(ConnectionFactory.class),
				ReferenceBackfillCatalog.load());
	}

	@Command(name = "verify", description = "Compare the broker and PostgreSQL entity by entity.")
	int verify(@Mixin MigrationContexts contexts,
			@Option(names = "--dry-run", description = DRY_RUN_DESCRIPTION) boolean dryRun) {
		return notImplemented("verify");
	}

	@Command(name = "cleanup-subscriptions", description = "Delete the broker subscriptions registered for TMForum hubs.")
	int cleanupSubscriptions(@Mixin MigrationContexts contexts,
			@Option(names = "--dry-run", description = DRY_RUN_DESCRIPTION) boolean dryRun) {
		return notImplemented("cleanup-subscriptions");
	}

	@Command(name = "replay", description = "Replay recorded API requests against both backends and diff the responses.")
	int replay(@Mixin MigrationContexts contexts,
			@Option(names = "--dry-run", description = DRY_RUN_DESCRIPTION) boolean dryRun) {
		return notImplemented("replay");
	}

	@Command(name = "export", description = "Copy every entity from PostgreSQL back into an empty broker (rollback).")
	int export(@Mixin MigrationContexts contexts,
			@Option(names = "--dry-run", description = DRY_RUN_DESCRIPTION) boolean dryRun) {
		return notImplemented("export");
	}

	private int notImplemented(String command) {
		spec.commandLine().getErr().println(String.format(NOT_IMPLEMENTED, command));
		return ExitCode.SOFTWARE;
	}
}
