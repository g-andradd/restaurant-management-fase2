package br.com.fiap.restaurant.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class ArchitecturePurityTest {

    private static final JavaClasses classes =
            new ClassFileImporter().importPackages("br.com.fiap.restaurant");

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
}
