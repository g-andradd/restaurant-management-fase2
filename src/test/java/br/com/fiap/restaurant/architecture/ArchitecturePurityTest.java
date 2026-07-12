package br.com.fiap.restaurant.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;

class ArchitecturePurityTest {

    private static final JavaClasses classes =
            new ClassFileImporter()
                    .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                    .importPackages("br.com.fiap.restaurant");

    @Test
    void domainMustNotDependOnFrameworks() {
        noClasses().that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework..", "jakarta.persistence..", "jakarta.validation..")
                .check(classes);
    }

    @Test
    void applicationMustNotDependOnSpring() {
        noClasses().that().resideInAPackage("..application..")
                .should().dependOnClassesThat().resideInAnyPackage("org.springframework..")
                .check(classes);
    }

    @Test
    void applicationAndDomainMustNotDependOnJjwt() {
        noClasses().that().resideInAnyPackage("..domain..", "..application..")
                .should().dependOnClassesThat().resideInAnyPackage("io.jsonwebtoken..")
                .check(classes);
    }

    @Test
    void mustNotUsePreAuthorizeAnnotations() {
        // Ownership/authorization rules (e.g. restaurant-owner checks) must live
        // in use cases as explicit, testable code - never as a @PreAuthorize
        // annotation on a controller method, which would hide the rule from
        // unit tests and reintroduce reliance on stale JWT claims.
        noMethods().should().beAnnotatedWith(PreAuthorize.class).check(classes);
    }
}
