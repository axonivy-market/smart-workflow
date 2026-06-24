package com.axonivy.utils.smart.workflow.demo.supplier.onboarding.helper;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.axonivy.utils.smart.workflow.demo.supplier.Supplier;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.OnboardingRequest;

import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
class TestOnboardingRequestHelper {

  @Test
  void ensureNestedObjectsExist_whenAllNull_createsAllNestedObjects() {
    OnboardingRequest request = new OnboardingRequest();

    OnboardingRequestHelper.ensureNestedObjectsExist(request);

    assertThat(request.getSupplier()).isNotNull();
    assertThat(request.getSupplier().getBusinessAddress()).isNotNull();
    assertThat(request.getSupplier().getPrimaryContact()).isNotNull();
    assertThat(request.getSupplier().getBanking()).isNotNull();
    assertThat(request.getSupplier().getCertifications()).isNotNull().isEmpty();
  }

  @Test
  void ensureNestedObjectsExist_whenAlreadyInitialized_doesNotOverwrite() {
    OnboardingRequest request = new OnboardingRequest();
    Supplier existing = new Supplier();
    existing.setBusinessName("Existing Supplier");
    request.setSupplier(existing);

    OnboardingRequestHelper.ensureNestedObjectsExist(request);

    assertThat(request.getSupplier()).isSameAs(existing);
    assertThat(request.getSupplier().getBusinessName()).isEqualTo("Existing Supplier");
  }

  @Test
  void ensureNestedObjectsExist_whenSupplierExistsButNestedFieldsNull_createsDefaults() {
    OnboardingRequest request = new OnboardingRequest();
    Supplier supplier = new Supplier();
    request.setSupplier(supplier);

    OnboardingRequestHelper.ensureNestedObjectsExist(request);

    assertThat(supplier.getBusinessAddress()).isNotNull();
    assertThat(supplier.getCertifications()).isNotNull().isEmpty();
  }

  @Test
  void ensureNestedObjectsExist_isIdempotent() {
    OnboardingRequest request = new OnboardingRequest();

    OnboardingRequestHelper.ensureNestedObjectsExist(request);
    Supplier supplierAfterFirst = request.getSupplier();

    OnboardingRequestHelper.ensureNestedObjectsExist(request);

    assertThat(request.getSupplier()).isSameAs(supplierAfterFirst);
  }
}
