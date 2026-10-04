package com.trivoko.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import java.util.Optional;

import org.springframework.data.repository.Repository;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

/**
 * TriVoKo's module rules. A module = the package right under com.trivoko
 * (catalog, seller, order, cart ...).
 *
 * <pre>
 *   order --calls--> catalog.ProductService      OK  (the catalog module decides what is allowed)
 *   order --calls--> catalog.ProductRepository   NOT OK (skips the catalog's rules)
 * </pre>
 *
 * Like a mall: a shop asks another shop's counter for goods; it never walks into their storeroom.
 */
final class ModuleRules {

	static final ArchRule REPOSITORIES_STAY_IN_THEIR_MODULE = classes()
		.that().resideInAPackage("com.trivoko..")
		.should(new ArchCondition<JavaClass>("use only the repositories of their own module") {
			@Override
			public void check(JavaClass origin, ConditionEvents events) {
				for (Dependency dependency : origin.getDirectDependenciesFromSelf()) {
					JavaClass target = dependency.getTargetClass();
					boolean isRepository = target.isAssignableTo(Repository.class) && !target.getPackageName().startsWith("org.springframework");
					if (isRepository && !module(origin).equals(module(target))) {
						events.add(SimpleConditionEvent.violated(dependency, dependency.getDescription()
								+ " -> module '" + module(origin).orElse("?") + "' uses a repository of module '"
								+ module(target).orElse("?") + "'; call its service instead"));
					}
				}
			}
		})
		.because("a module must go through another module's service, never its repository");

	/** com.trivoko.catalog.dto.ProductCard -> "catalog"; com.trivoko.TrivokoApplication -> empty. */
	static Optional<String> module(JavaClass javaClass) {
		String rest = javaClass.getPackageName().replaceFirst("^com\\.trivoko\\.?", "");
		return rest.isEmpty() ? Optional.empty() : Optional.of(rest.split("\\.")[0]);
	}

	private ModuleRules() {
	}

}
