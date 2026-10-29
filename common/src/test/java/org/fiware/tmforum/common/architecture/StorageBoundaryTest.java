package org.fiware.tmforum.common.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import org.fiware.tmforum.common.repository.BrokerBackedEntitiesRepository;
import org.fiware.tmforum.common.repository.BrokerBackedReferencesRepository;
import org.fiware.tmforum.common.repository.NgsiLdBaseRepository;
import org.fiware.tmforum.common.repository.NgsiLdEntityRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Keeps {@code common} on the storage port: only the adapter package and the IT helpers may touch the NGSI-LD
 * adapter, and no REST class may branch on broker responses.
 */
class StorageBoundaryTest {

	private static final String COMMON_PACKAGE = "org.fiware.tmforum.common";
	private static final String ADAPTER_PACKAGE = "org.fiware.tmforum.common.repository..";
	private static final String IT_HELPER_PACKAGE = "org.fiware.tmforum.common.test..";
	private static final String REST_PACKAGES = "..rest..";

	private static JavaClasses mainClasses;

	@BeforeAll
	static void importMainClasses() {
		mainClasses = new ClassFileImporter()
				.withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
				.importPackages(COMMON_PACKAGE);
	}

	static Stream<Named<ArchRule>> rules() {
		return Stream.of(
						noClasses().that().resideOutsideOfPackages(ADAPTER_PACKAGE, IT_HELPER_PACKAGE)
								.should().dependOnClassesThat().belongToAnyOf(
										NgsiLdEntityRepository.class, NgsiLdBaseRepository.class,
										BrokerBackedReferencesRepository.class, BrokerBackedEntitiesRepository.class),
						noClasses().that().resideInAPackage(REST_PACKAGES)
								.should().dependOnClassesThat().areAssignableTo(HttpClientResponseException.class))
				.map(rule -> Named.of(rule.getDescription(), rule));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("rules")
	void ruleHolds(ArchRule rule) {
		rule.check(mainClasses);
	}
}
