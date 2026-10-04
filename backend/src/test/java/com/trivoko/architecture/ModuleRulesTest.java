package com.trivoko.architecture;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.trivoko.catalog.ProductRepository;

/**
 * The modular-monolith rule, checked on every build (and so in CI):
 * a module may call another module's SERVICE, never its REPOSITORY.
 */
class ModuleRulesTest {

	@Test
	void noModuleUsesAnotherModulesRepository() {
		JavaClasses mainCode = new ClassFileImporter()
			.withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
			.importPackages("com.trivoko");

		ModuleRules.REPOSITORIES_STAY_IN_THEIR_MODULE.check(mainCode);
	}

	/** Proof that the rule is not empty: a class that breaks it must be reported. */
	@Test
	void ruleCatchesAModuleUsingAnotherModulesRepository() throws ClassNotFoundException {
		JavaClasses breaker = new ClassFileImporter().importClasses(
				Class.forName("com.trivoko.order.ModuleRuleBreaker"), ProductRepository.class);

		assertThatThrownBy(() -> ModuleRules.REPOSITORIES_STAY_IN_THEIR_MODULE.check(breaker))
			.isInstanceOf(AssertionError.class)
			.hasMessageContaining("com.trivoko.order.ModuleRuleBreaker")
			.hasMessageContaining("ProductRepository");
	}

}
