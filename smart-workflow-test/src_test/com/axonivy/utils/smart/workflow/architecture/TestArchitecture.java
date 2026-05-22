package com.axonivy.utils.smart.workflow.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ClassFileImporter;

public class TestArchitecture {

  @Test
  void internalSmartWorkflowPackages_areNotUsedInConsumingBundles() throws IOException {
    var smartWorkflowClasses = new ClassFileImporter()
        .importPath(SmartProjects.core());
    var consumerClasses = new ClassFileImporter()
        .importPaths(SmartProjects.consumers());

    var smartWorkflowInternalPackages = smartWorkflowClasses.stream()
        .map(JavaClass::getPackageName)
        .filter(pkg -> pkg.contains(".internal"))
        .distinct()
        .toList();

    var resideInSmartWorkflowInternalPackages = new DescribedPredicate<JavaClass>(
        "reside in smart-workflow 'internal' packages") {
      @Override
      public boolean test(JavaClass input) {
        return smartWorkflowInternalPackages.stream()
            .anyMatch(pkg -> input.getPackageName().equals(pkg) || input.getPackageName().startsWith(pkg + "."));
      }
    };

    noClasses()
        .should().dependOnClassesThat(resideInSmartWorkflowInternalPackages)
        .because("smart-workflow internal packages are reserved for smart-workflow-test")
        .check(consumerClasses);
  }

  @Test
  void modelsPackages_areNotUsedInConsumingBundles() throws IOException {
    var modelClasses = new ClassFileImporter()
        .importPaths(SmartProjects.models());
    var consumerClasses = new ClassFileImporter()
        .importPaths(SmartProjects.consumersNoModels());

    var modelPackages = modelClasses.stream()
        .map(JavaClass::getPackageName)
        .distinct()
        .toList();

    var resideInModelPackages = new DescribedPredicate<JavaClass>("reside in 'model' packages") {
      @Override
      public boolean test(JavaClass input) {
        return modelPackages.stream()
            .anyMatch(pkg -> input.getPackageName().equals(pkg) || input.getPackageName().startsWith(pkg + "."));
      }
    };

    noClasses()
        .should().dependOnClassesThat(resideInModelPackages)
        .because("models are only accessible through SPI; direct access is prohibited")
        .check(consumerClasses);
  }

}
