package br.com.fiap.restaurant.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

class LayeredArchitectureTest {

    private static final JavaClasses classes =
            new ClassFileImporter()
                    .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                    .importPackages("br.com.fiap.restaurant");

    @Test
    void layersRespectDependencyRule() {
        layeredArchitecture()
                .consideringOnlyDependenciesInAnyPackage("br.com.fiap.restaurant..")
                .layer("Domain").definedBy("br.com.fiap.restaurant.domain..")
                .layer("Application").definedBy("br.com.fiap.restaurant.application..")
                .layer("Infrastructure").definedBy("br.com.fiap.restaurant.infrastructure..")
                .whereLayer("Domain").mayNotAccessAnyLayer()
                .whereLayer("Application").mayOnlyAccessLayers("Domain")
                .whereLayer("Infrastructure").mayOnlyAccessLayers("Domain", "Application")
                .check(classes);
    }
}
