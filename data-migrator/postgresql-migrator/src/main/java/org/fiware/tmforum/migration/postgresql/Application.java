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

import java.io.PrintWriter;
import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Moves a broker-backed deployment onto PostgreSQL and back, and backfills inline references on either backend.
 */
@Command(name = "postgresql-migrator", mixinStandardHelpOptions = true,
		description = "Migrates TMForum data from an NGSI-LD broker to PostgreSQL.")
public class Application implements Runnable {

	private static final String DRY_RUN_DESCRIPTION = "Report what would change without writing.";
	private static final String DEFAULT_PAGE_SIZE = "100";
	private static final String DEFAULT_SAMPLE_SIZE = "50";
	private static final String DEFAULT_FULL_CHECK_THRESHOLD = "1000";
	private static final String TARGET_POSTGRESQL = "postgresql";
	private static final String TARGET_BROKER = "broker";
	static final String CONFIRM_CUTOVER_FINAL = "--confirm-cutover-final";
	static final String CONFIRMATION_MISSING = "cleanup-subscriptions breaks hub notifications on the broker, which a "
			+ "rollback relies on. Run it after the rollback window with " + CONFIRM_CUTOVER_FINAL + ", or use --dry-run.";
	private static final String DEFAULT_REPLAY_TIMEOUT_SECONDS = "30";
	private static final String DEFAULT_REPLAY_REPORT = "replay-report.json";
	static final String BACKFILL_SKIPPED_FAILED_IMPORT = "Reference backfill skipped: the import failed.";
	// the backfill fills from the stored targets, and a dry-run import stored none
	static final String BACKFILL_SKIPPED_DRY_RUN =
			"Reference backfill skipped on a dry run: nothing was imported to fill from.";

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
					+ "backfill-references --target " + TARGET_POSTGRESQL + "; skipped on a dry run.") boolean backfillReferences) {
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
			ImportReport report = new ImportCommand(new BrokerSource(contexts.source(), registry),
					new PostgresTarget(contexts.target()), registry, options).run(resolvedTypes);
			recordEscaperDrift(contexts, report::escaperDrift);
			Supplier<ReferenceBackfillReport> backfill = backfillReferences
					? () -> runPostgresBackfill(contexts, dryRun)
					: null;
			return importThenBackfill(report, backfill, dryRun, spec.commandLine().getOut());
		}
	}

	/**
	 * Prints the import report, then runs and prints the backfill when one is requested ({@code backfill} non-null),
	 * the import succeeded and it is not a dry run. Returns the exit code of both.
	 */
	static int importThenBackfill(ImportReport report, Supplier<ReferenceBackfillReport> backfill, boolean dryRun,
			PrintWriter out) {
		report.print(out);
		if (backfill == null) {
			return exitCode(report.succeeded());
		}
		if (!report.succeeded()) {
			out.println(BACKFILL_SKIPPED_FAILED_IMPORT);
			out.flush();
			return ExitCode.SOFTWARE;
		}
		if (dryRun) {
			out.println(BACKFILL_SKIPPED_DRY_RUN);
			out.flush();
			return ExitCode.OK;
		}
		ReferenceBackfillReport backfillReport = backfill.get();
		backfillReport.print(out);
		return exitCode(backfillReport.succeeded());
	}

	@Command(name = "backfill-references", description = "Fill absent fields of stored references from their targets.")
	int backfillReferences(@Mixin MigrationContexts contexts,
			@Option(names = "--target", required = true,
					description = "Store to backfill: " + TARGET_POSTGRESQL + " or " + TARGET_BROKER + ".") String target,
			@Option(names = "--dry-run", description = DRY_RUN_DESCRIPTION) boolean dryRun,
			@Option(names = "--page-size", defaultValue = DEFAULT_PAGE_SIZE,
					description = "Entities per broker request (" + TARGET_BROKER + " only); Orion-LD caps it at 1000.")
			int pageSize) {
		if (TARGET_BROKER.equals(target)) {
			if (pageSize < 1) {
				throw new ParameterException(spec.commandLine(), "--page-size must be positive.");
			}
			contexts.brokerWriteModeProblem().ifPresent(problem -> {
				throw new ParameterException(spec.commandLine(), problem);
			});
			try (contexts) {
				contexts.startSource();
				ReferenceBackfillReport report = new BrokerReferenceBackfill(contexts.source(),
						ReferenceBackfillCatalog.load(), EntityTypeRegistry.scan(), pageSize).run(dryRun);
				// it maps broker data, which only decodes right under --source-escaped
				recordEscaperDrift(contexts, report::error);
				report.print(spec.commandLine().getOut());
				return exitCode(report.succeeded());
			}
		}
		if (!TARGET_POSTGRESQL.equals(target)) {
			throw new ParameterException(spec.commandLine(), String.format(
					"Unknown --target '%s', expected %s or %s.", target, TARGET_POSTGRESQL, TARGET_BROKER));
		}
		try (contexts) {
			contexts.startTarget();
			ReferenceBackfillReport report = runPostgresBackfill(contexts, dryRun);
			report.print(spec.commandLine().getOut());
			return exitCode(report.succeeded());
		}
	}

	// pure SQL that decodes nothing, so no escaper check: a PostgreSQL-only run never sets the flag
	private static ReferenceBackfillReport runPostgresBackfill(MigrationContexts contexts, boolean dryRun) {
		return new PostgresReferenceBackfill(
				contexts.target().getBean(ConnectionFactory.class), ReferenceBackfillCatalog.load()).run(dryRun);
	}

	private static void recordEscaperDrift(MigrationContexts contexts, Consumer<String> report) {
		try {
			contexts.verifyEscaperFlag();
		} catch (IllegalStateException e) {
			report.accept(e.getMessage());
		}
	}

	private static int exitCode(boolean succeeded) {
		return succeeded ? ExitCode.OK : ExitCode.SOFTWARE;
	}

	@Command(name = "verify", description = "Compare the broker and PostgreSQL entity by entity.")
	int verify(@Mixin MigrationContexts contexts,
			@Option(names = "--dry-run", description = "Accepted for uniformity; verify never writes.") boolean dryRun,
			@Option(names = "--types", split = ",", description = "Entity types to verify; all mapped types by default.")
			List<String> types,
			@Option(names = "--page-size", defaultValue = DEFAULT_PAGE_SIZE,
					description = "Entities per broker request; Orion-LD caps it at 1000.") int pageSize,
			@Option(names = "--sample-size", defaultValue = DEFAULT_SAMPLE_SIZE,
					description = "Entities per type that the field-level diff checks.") int sampleSize,
			@Option(names = "--seed", description = "Seed of the sample, for a reproducible run; random by default "
					+ "and printed in the report.") Long seed,
			@Option(names = "--full-check-threshold", defaultValue = DEFAULT_FULL_CHECK_THRESHOLD,
					description = "Types with at most this many entities are diffed completely.") int fullCheckThreshold,
			@Option(names = "--report", description = "Write the JSON report to this file.") Path reportFile,
			@Option(names = "--direction", defaultValue = "import", converter = DirectionConverter.class,
					description = "import (broker to PostgreSQL) or export (PostgreSQL to the broker, which allows "
							+ "the hubs' newly registered subscriptions).") VerifyOptions.Direction direction) {
		VerifyOptions options;
		Set<String> resolvedTypes;
		EntityTypeRegistry registry = EntityTypeRegistry.scan();
		try {
			options = new VerifyOptions(pageSize, sampleSize,
					seed == null ? ThreadLocalRandom.current().nextLong() : seed, fullCheckThreshold,
					contexts.sourceEscaped, direction);
			resolvedTypes = ImportCommand.resolveTypes(registry, types);
		} catch (IllegalArgumentException e) {
			throw new ParameterException(spec.commandLine(), e.getMessage());
		}
		try (contexts) {
			contexts.start();
			VerifyReport report = new VerifyCommand(new BrokerSource(contexts.source(), registry),
					new PostgresVerifyTarget(contexts.target()), registry, ReferenceBackfillCatalog.load(), options)
					.run(resolvedTypes);
			recordEscaperDrift(contexts, report::error);
			if (reportFile != null) {
				report.write(reportFile);
			}
			report.print(spec.commandLine().getOut());
			return exitCode(report.succeeded());
		}
	}

	@Command(name = "cleanup-subscriptions", description = "Delete the broker subscriptions registered for TMForum hubs "
			+ "and drop the hubs' subscription attribute in PostgreSQL. Breaks hub notifications on the broker, so run "
			+ "it only once the rollback window is over.")
	int cleanupSubscriptions(@Mixin MigrationContexts contexts,
			@Option(names = "--dry-run", description = "List what would be deleted without writing; needs no "
					+ CONFIRM_CUTOVER_FINAL + ".") boolean dryRun,
			@Option(names = CONFIRM_CUTOVER_FINAL, description = "Confirm the cutover to PostgreSQL is final: no "
					+ "rollback to the broker will follow.") boolean confirmCutoverFinal,
			@Option(names = "--include-orphans", description = "Also delete broker subscriptions that no hub references "
					+ "but that notify a --callback-endpoint.") boolean includeOrphans,
			@Option(names = "--callback-endpoint", split = ",", description = "<serverHost><basepath>/callback of a "
					+ "TMForum service that registered subscriptions on the broker.") Set<String> callbackEndpoints,
			@Option(names = "--page-size", defaultValue = DEFAULT_PAGE_SIZE,
					description = "Entities and subscriptions per broker request; Orion-LD caps it at 1000.") int pageSize) {
		if (!dryRun && !confirmCutoverFinal) {
			throw new ParameterException(spec.commandLine(), CONFIRMATION_MISSING);
		}
		CleanupSubscriptionsCommand.Options options;
		try {
			options = new CleanupSubscriptionsCommand.Options(pageSize, dryRun, includeOrphans,
					callbackEndpoints == null ? Set.of() : callbackEndpoints);
		} catch (IllegalArgumentException e) {
			throw new ParameterException(spec.commandLine(), e.getMessage());
		}
		try (contexts) {
			contexts.start();
			BrokerSource broker = new BrokerSource(contexts.source(), EntityTypeRegistry.scan());
			CleanupSubscriptionsReport report = new CleanupSubscriptionsCommand(broker, broker,
					new PostgresStoredHubs(contexts.target().getBean(ConnectionFactory.class)), options).run();
			// it maps the broker's hubs
			recordEscaperDrift(contexts, report::error);
			report.print(spec.commandLine().getOut());
			return exitCode(report.succeeded());
		}
	}

	@Command(name = "replay", description = "Send recorded API requests to the broker-backed and the PostgreSQL-backed "
			+ "deployment and diff the responses.")
	int replay(@Option(names = "--requests", required = true,
					description = "JSON-lines file, one captured request per line: method, path, headers, body.")
			Path requests,
			@Option(names = "--broker-base-url", required = true,
					description = "Base URL of the broker-backed deployment.") URI brokerBaseUrl,
			@Option(names = "--postgres-base-url", required = true,
					description = "Base URL of the PostgreSQL-backed deployment.") URI postgresBaseUrl,
			@Option(names = "--unordered-paths", split = ",", description = "Request paths whose response lists are "
					+ "compared as sets; * matches within a path segment, ** across segments.") List<String> unorderedPaths,
			@Option(names = "--ignore", split = ",", description = "JSON pointers left out of the diff; * matches one "
					+ "key or index, ** any depth.") List<String> ignore,
			@Option(names = "--external-host", split = ",", description = "Hosts of external targets "
					+ "(references.external-hosts): only id and href of their references are compared.")
			Set<String> externalHosts,
			@Option(names = "--include-writes", description = "Also replay POST, PUT, PATCH and DELETE; for rehearsal "
					+ "environments only.") boolean includeWrites,
			@Option(names = "--timeout-seconds", defaultValue = DEFAULT_REPLAY_TIMEOUT_SECONDS,
					description = "Per-request timeout in seconds.") long timeoutSeconds,
			@Option(names = "--report", defaultValue = DEFAULT_REPLAY_REPORT,
					description = "Where the JSON report goes.") Path reportFile,
			@Option(names = "--dry-run", description = "List what would be sent without sending.") boolean dryRun) {
		ReplayCommand command;
		try {
			command = new ReplayCommand(new HttpReplayClient(Duration.ofSeconds(timeoutSeconds)), new ReplayOptions(brokerBaseUrl, postgresBaseUrl,
					unorderedPaths, ignore, externalHosts, includeWrites, dryRun));
		} catch (IllegalArgumentException e) {
			throw new ParameterException(spec.commandLine(), e.getMessage());
		}
		ReplayReport report = command.run(requests);
		report.write(reportFile);
		report.print(spec.commandLine().getOut());
		return exitCode(report.succeeded());
	}

	@Command(name = "export", description = "Copy every entity from PostgreSQL back into an empty broker (rollback), "
			+ "registering a new NGSI-LD subscription for every hub.")
	int export(@Mixin MigrationContexts contexts,
			@Option(names = "--dry-run", description = DRY_RUN_DESCRIPTION) boolean dryRun,
			@Option(names = "--types", split = ",", description = "Entity types to export; all mapped types by default.")
			List<String> types,
			@Option(names = "--page-size", defaultValue = DEFAULT_PAGE_SIZE,
					description = "Entities per PostgreSQL page and subscriptions per broker request.") int pageSize,
			@Option(names = "--resume", description = "Continue an earlier export: accept a non-empty broker and skip "
					+ "ids that are already stored, never overwriting them; implies --keep-broker-subscriptions. A hub "
					+ "reuses the subscription an interrupted run registered for it; a stored hub whose subscription is "
					+ "gone fails. Only for a broker that holds nothing but an earlier export's output.") boolean resume,
			@Option(names = "--keep-broker-subscriptions", description = "Keep the hub subscriptions the broker "
					+ "already holds, e.g. the old broker's in the rollback window, instead of refusing them; the "
					+ "broker must still hold no entity of an exported type. Kept subscriptions are live for the whole "
					+ "export and notify for every exported entity of the types they watch.") boolean keepBrokerSubscriptions,
			@Option(names = "--fail-fast", description = "Stop at the first failure.") boolean failFast,
			@Option(names = "--notification-endpoint", description = "<serverHost><basepath>/callback of the "
					+ "broker-backed deployment, notified by the hubs' new subscriptions; required when hubs are "
					+ "exported.") URI notificationEndpoint,
			@Option(names = "--notification-endpoint-for", description = "<entityType>=<url>: the endpoint for hubs "
					+ "listening to that type, for deployments split per API module.")
			Map<String, URI> endpointsByType) {
		ExportOptions options;
		Set<String> resolvedTypes;
		EntityTypeRegistry registry = EntityTypeRegistry.scan();
		try {
			options = new ExportOptions(pageSize, resume, keepBrokerSubscriptions, failFast, dryRun, notificationEndpoint,
					endpointsByType);
			resolvedTypes = ImportCommand.resolveTypes(registry, types);
		} catch (IllegalArgumentException e) {
			throw new ParameterException(spec.commandLine(), e.getMessage());
		}
		try (contexts) {
			contexts.start();
			ExportReport report = new ExportCommand(new PostgresExportSource(contexts.target()),
					new BrokerExportTarget(contexts.source(), new BrokerSource(contexts.source(), registry)), registry,
					options).run(resolvedTypes);
			// it encodes the broker's hub queries, which must match --source-escaped
			recordEscaperDrift(contexts, report::error);
			report.print(spec.commandLine().getOut());
			return exitCode(report.succeeded());
		}
	}

	/**
	 * Reads {@code --direction} case-insensitively.
	 */
	static final class DirectionConverter implements CommandLine.ITypeConverter<VerifyOptions.Direction> {
		@Override
		public VerifyOptions.Direction convert(String value) {
			return VerifyOptions.Direction.valueOf(value.toUpperCase(Locale.ROOT));
		}
	}
}
