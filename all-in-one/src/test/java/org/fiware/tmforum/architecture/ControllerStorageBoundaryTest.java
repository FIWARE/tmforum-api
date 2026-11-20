package org.fiware.tmforum.architecture;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.syntax.elements.GivenClassesConjunction;
import org.fiware.tmforum.common.notification.EntityEventPublisher;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Set;
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
	private static final String POSTGRES_ADAPTER_PACKAGES = "org.fiware.tmforum.storage.postgresql..";
	private static final Set<String> WRITE_METHODS = Set.of("createDomainEntity", "updateDomainEntity",
			"deleteDomainEntity");
	// writes related entities without events on purpose; its lambdas compile to lambda$<name>$<n>
	private static final String EVENTLESS_WRITER = "relatedEntityHandlingMono";
	private static final String LAMBDA_PREFIX = "lambda$";

	private static JavaClasses mainClasses;

	@BeforeAll
	static void importMainClasses() {
		mainClasses = new ClassFileImporter()
				.withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
				.importPackages(ROOT_PACKAGE);
	}

	static Stream<Named<ArchRule>> rules() {
		// The whole package is adapter-private: only its interfaces are public API, so any concrete type is blocked.
		DescribedPredicate<JavaClass> adapterPrivateTypes = resideInAPackage(ADAPTER_PACKAGES).and(not(INTERFACES))
				.as("are non-interface types in the adapter-private package " + ADAPTER_PACKAGES);
		DescribedPredicate<JavaMethodCall> eventlessWrites = DescribedPredicate.describe(
				"a storage write outside " + EVENTLESS_WRITER,
				call -> WRITE_METHODS.contains(call.getName()) && !isEventlessWriter(call.getOrigin().getName()));
		return Stream.of(
						noModuleClasses().should().dependOnClassesThat(adapterPrivateTypes),
						// every API write goes through EntityEventPublisher, so each backend can attach its events
						noClasses().that().resideInAPackage(ALL_PACKAGES)
								.and().resideOutsideOfPackages(ADAPTER_PACKAGES, POSTGRES_ADAPTER_PACKAGES)
								.and().doNotImplement(EntityEventPublisher.class)
								.should().callMethodWhere(eventlessWrites),
						noModuleClasses().should().dependOnClassesThat().resideInAPackage(NGSI_PACKAGES),
						// Inert until Step 7 puts storage-postgresql on this module's classpath; product-catalog's
						// StorageBoundaryTest enforces it meanwhile.
						noModuleClasses().and().resideOutsideOfPackage(POSTGRES_ADAPTER_PACKAGES)
								.should().dependOnClassesThat().resideInAPackage(POSTGRES_ADAPTER_PACKAGES))
				.map(rule -> Named.of(rule.getDescription(), rule));
	}

	private static boolean isEventlessWriter(String methodName) {
		return methodName.equals(EVENTLESS_WRITER) || methodName.startsWith(LAMBDA_PREFIX + EVENTLESS_WRITER + "$");
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
