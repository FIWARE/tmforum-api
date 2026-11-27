package org.fiware.tmforum.accesspolicy.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Keeps {@code access-policy} domain-agnostic: no TMForum domain, common or NGSI-LD class, so a carrier for a domain
 * type can only live in a module that already knows the type.
 */
class DomainBoundaryTest {

	private static final String ACCESS_POLICY_PACKAGE = "org.fiware.tmforum.accesspolicy";
	private static final String ACCESS_POLICY_PACKAGES = ACCESS_POLICY_PACKAGE + "..";
	private static final String STORAGE_PACKAGES = "org.fiware.tmforum.storage..";
	private static final String TMFORUM_PACKAGES = "org.fiware.tmforum..";
	private static final String MAPPING_PACKAGES = "io.github.wistefan..";
	private static final String NGSI_PACKAGES = "org.fiware.ngsi..";

	private static JavaClasses mainClasses;

	@BeforeAll
	static void importMainClasses() {
		mainClasses = new ClassFileImporter()
				.withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
				.importPackages(ACCESS_POLICY_PACKAGE);
	}

	static Stream<Named<ArchRule>> rules() {
		return Stream.of(
						noClasses().that().resideInAPackage(ACCESS_POLICY_PACKAGES)
								.should().dependOnClassesThat(resideInAPackage(TMFORUM_PACKAGES)
										.and(not(resideInAnyPackage(ACCESS_POLICY_PACKAGES, STORAGE_PACKAGES)))),
						noClasses().that().resideInAPackage(ACCESS_POLICY_PACKAGES)
								.should().dependOnClassesThat().resideInAnyPackage(MAPPING_PACKAGES, NGSI_PACKAGES))
				.map(rule -> Named.of(rule.getDescription(), rule));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("rules")
	void ruleHolds(ArchRule rule) {
		rule.check(mainClasses);
	}
}
