package org.fiware.tmforum.migration.postgresql;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Spec;

/**
 * Moves a broker-backed deployment onto PostgreSQL and backfills inline references on either backend.
 * The subcommands are stubs until their plan steps land; each one exits non-zero.
 */
@Command(name = "postgresql-migrator", mixinStandardHelpOptions = true,
		description = "Migrates TMForum data from an NGSI-LD broker to PostgreSQL.")
public class Application implements Runnable {

	private static final String DRY_RUN_DESCRIPTION = "Report what would change without writing.";
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

	@Command(name = "import", description = "Copy every entity from the broker into PostgreSQL.")
	int importEntities(@Mixin MigrationContexts contexts,
			@Option(names = "--dry-run", description = DRY_RUN_DESCRIPTION) boolean dryRun) {
		return notImplemented("import");
	}

	@Command(name = "backfill-references", description = "Fill absent fields of stored references from their targets.")
	int backfillReferences(@Mixin MigrationContexts contexts,
			@Option(names = "--dry-run", description = DRY_RUN_DESCRIPTION) boolean dryRun) {
		return notImplemented("backfill-references");
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
