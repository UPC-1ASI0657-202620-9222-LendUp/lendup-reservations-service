package com.lendup.reservas.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "com.lendup.reservas",
    importOptions = ImportOption.DoNotIncludeTests.class)
class HexagonalArchitectureTest {
  @ArchTest
  static final ArchRule domain_is_framework_independent = noClasses()
      .that().resideInAPackage("..domain..")
      .should().dependOnClassesThat().resideInAnyPackage(
          "..application..", "..infrastructure..", "..interfaces..", "..config..",
          "org.springframework..", "jakarta.persistence..", "org.hibernate..");

  @ArchTest
  static final ArchRule application_does_not_depend_on_adapters = noClasses()
      .that().resideInAPackage("..application..")
      .should().dependOnClassesThat().resideInAnyPackage(
          "..infrastructure..", "..interfaces..");
}
