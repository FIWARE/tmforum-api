package org.fiware.tmforum.architecture;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.syntax.elements.GivenClassesConjunction;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.INTERFACES;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Keeps every API module on the storage port. all-in-one compiles all module sources, so one import sees them all.
 */
class ControllerStorageBoundaryTest {

	private static final String ROOT_PACKAGE = "org.fiware.tmforum";
	private static final String ALL_PACKAGES = "org.fiware.tmforum..";
	private static final String COMMON_PACKAGES = "org.fiware.tmforum.common..";
	private static final String ADAPTER_PACKAGES = "org.fiware.tmforum.common.repository..";
	private static final String NGSI_PACKAGES = "org.fiware.ngsi..";

	private static JavaClasses mainClasses;

	@BeforeAll
	static void importMainClasses() {
		mainClasses = new ClassFileImporter()
				.withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
				.importPackages(ROOT_PACKAGE);
	}

	static Stream<Named<ArchRule>> rules() {
		DescribedPredicate<JavaClass> concreteRepositories = resideInAPackage(ADAPTER_PACKAGES).and(not(INTERFACES))
				.as("are concrete repositories in " + ADAPTER_PACKAGES);
		return Stream.of(
						noModuleClasses().should().dependOnClassesThat(concreteRepositories),
						noModuleClasses().should().dependOnClassesThat().resideInAPackage(NGSI_PACKAGES))
				.map(rule -> Named.of(rule.getDescription(), rule));
	}

	private static GivenClassesConjunction noModuleClasses() {
		return noClasses().that().resideInAPackage(ALL_PACKAGES).and().resideOutsideOfPackage(COMMON_PACKAGES);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("rules")
	void ruleHolds(ArchRule rule) {
		rule.check(mainClasses);
	}
}
