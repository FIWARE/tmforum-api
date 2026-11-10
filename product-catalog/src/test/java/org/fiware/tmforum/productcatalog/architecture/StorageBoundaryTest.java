package org.fiware.tmforum.productcatalog.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Keeps product-catalog on the storage port. It is the first API module with {@code storage-postgresql} on its
 * classpath, so this is where the "nothing outside the adapter touches it" rule can actually fail.
 */
class StorageBoundaryTest {

	private static final String ROOT_PACKAGE = "org.fiware.tmforum";
	private static final String POSTGRES_ADAPTER_PACKAGES = "org.fiware.tmforum.storage.postgresql..";

	@Test
	void onlyThePostgresAdapterUsesItself() {
		JavaClasses mainClasses = new ClassFileImporter()
				.withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
				.importPackages(ROOT_PACKAGE);

		noClasses().that().resideOutsideOfPackage(POSTGRES_ADAPTER_PACKAGES)
				.should().dependOnClassesThat().resideInAPackage(POSTGRES_ADAPTER_PACKAGES)
				.check(mainClasses);
	}
}
