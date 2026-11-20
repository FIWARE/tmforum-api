package org.fiware.tmforum.migration.postgresql;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import picocli.CommandLine.ExitCode;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApplicationTest {

	private static final String IMPORT_FAILED = "Import FAILED.";
	private static final String BACKFILL_SUCCEEDED = "Reference backfill succeeded.";
	private static final String BACKFILL_FAILED = "Reference backfill FAILED.";

	private final StringWriter out = new StringWriter();
	private final AtomicInteger backfillRuns = new AtomicInteger();

	@ParameterizedTest(name = "import succeeded={0}, dry run={1}, backfill succeeded={2}")
	@CsvSource({
			"true,  false, true,  1, true,  " + BACKFILL_SUCCEEDED,
			"true,  false, false, 1, false, " + BACKFILL_FAILED,
			"false, false, true,  0, false, " + Application.BACKFILL_SKIPPED_FAILED_IMPORT,
			"true,  true,  true,  0, true,  " + Application.BACKFILL_SKIPPED_DRY_RUN,
			"false, true,  true,  0, false, " + Application.BACKFILL_SKIPPED_FAILED_IMPORT})
	void runsTheBackfillOnlyAfterASuccessfulImport(boolean importSucceeded, boolean dryRun, boolean backfillSucceeded,
			int expectedBackfillRuns, boolean expectedSuccess, String expectedOutput) {
		int exitCode = Application.importThenBackfill(importReport(importSucceeded, dryRun),
				backfill(backfillSucceeded), dryRun, new PrintWriter(out));

		assertEquals(expectedBackfillRuns, backfillRuns.get(), out.toString());
		assertEquals(exitCode(expectedSuccess), exitCode, out.toString());
		assertTrue(out.toString().contains(expectedOutput), out.toString());
	}

	@ParameterizedTest(name = "import succeeded={0}")
	@ValueSource(booleans = {true, false})
	void runsNoBackfillWithoutTheFlag(boolean importSucceeded) {
		int exitCode = Application.importThenBackfill(importReport(importSucceeded, false), null, false,
				new PrintWriter(out));

		assertEquals(exitCode(importSucceeded), exitCode, out.toString());
		assertFalse(out.toString().contains("Reference backfill"), out.toString());
	}

	@Test
	void printsBothReportsInOrder() {
		Application.importThenBackfill(importReport(true, false), backfill(true), false, new PrintWriter(out));

		String printed = out.toString();
		assertTrue(printed.indexOf("Import succeeded.") < printed.indexOf(BACKFILL_SUCCEEDED), printed);
		assertFalse(printed.contains(IMPORT_FAILED), printed);
	}

	private static int exitCode(boolean success) {
		return success ? ExitCode.OK : ExitCode.SOFTWARE;
	}

	private static ImportReport importReport(boolean succeeded, boolean dryRun) {
		ImportReport report = new ImportReport(dryRun);
		if (!succeeded) {
			report.abort();
		}
		return report;
	}

	private Supplier<ReferenceBackfillReport> backfill(boolean succeeded) {
		return () -> {
			backfillRuns.incrementAndGet();
			ReferenceBackfillReport report = new ReferenceBackfillReport(false);
			if (!succeeded) {
				report.fail("broken");
			}
			return report;
		};
	}
}
