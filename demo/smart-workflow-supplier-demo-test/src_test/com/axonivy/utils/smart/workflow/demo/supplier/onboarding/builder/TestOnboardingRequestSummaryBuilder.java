package com.axonivy.utils.smart.workflow.demo.supplier.onboarding.builder;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.axonivy.utils.smart.workflow.demo.common.Address;
import com.axonivy.utils.smart.workflow.demo.supplier.Supplier;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.OnboardingRequest;

import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
class TestOnboardingRequestSummaryBuilder {

  @Test
  void build_emptyRequestAndNullSupplier_omitsLines() {
    assertThat(OnboardingRequestSummaryBuilder.build(new OnboardingRequest())).isEmpty();

    OnboardingRequest req = new OnboardingRequest();
    req.setRequestedBy("Jane");
    assertThat(OnboardingRequestSummaryBuilder.build(req))
        .noneMatch(l -> "Supplier".equals(l.getLabel()))
        .noneMatch(l -> "Location".equals(l.getLabel()));
  }

  @Test
  void build_requestedBy_includedOrOmittedWhenBlank() {
    OnboardingRequest present = new OnboardingRequest();
    present.setRequestedBy("John Doe");
    assertThat(OnboardingRequestSummaryBuilder.build(present))
        .anyMatch(l -> "Requested by".equals(l.getLabel()) && "John Doe".equals(l.getValue()));

    OnboardingRequest blank = new OnboardingRequest();
    blank.setRequestedBy("  ");
    assertThat(OnboardingRequestSummaryBuilder.build(blank))
        .noneMatch(l -> "Requested by".equals(l.getLabel()));
  }

  @Test
  void build_withAnnualVolume_formatsAsEur() {
    OnboardingRequest request = new OnboardingRequest();
    request.setExpectedAnnualVolume(50000.0);

    assertThat(OnboardingRequestSummaryBuilder.build(request))
        .anyMatch(l -> "Annual volume".equals(l.getLabel()) && l.getValue().startsWith("EUR "));
  }

  @Test
  void build_supplier_withAndWithoutVat() {
    OnboardingRequest withVat = new OnboardingRequest();
    Supplier s1 = new Supplier();
    s1.setBusinessName("ACME Corp");
    s1.setVatId("DE123456789");
    withVat.setSupplier(s1);
    assertThat(OnboardingRequestSummaryBuilder.build(withVat))
        .anyMatch(l -> "Supplier".equals(l.getLabel())
            && l.getValue().contains("ACME Corp") && l.getValue().contains("DE123456789"));

    OnboardingRequest noVat = new OnboardingRequest();
    Supplier s2 = new Supplier();
    s2.setBusinessName("No VAT Ltd");
    noVat.setSupplier(s2);
    assertThat(OnboardingRequestSummaryBuilder.build(noVat))
        .anyMatch(l -> "Supplier".equals(l.getLabel()) && "No VAT Ltd".equals(l.getValue()));
  }

  @Test
  void build_location_withAndWithoutCountry() {
    OnboardingRequest withCountry = new OnboardingRequest();
    Supplier s1 = new Supplier();
    Address a1 = new Address();
    a1.setCity("Berlin");
    a1.setCountry("Germany");
    s1.setBusinessAddress(a1);
    withCountry.setSupplier(s1);
    assertThat(OnboardingRequestSummaryBuilder.build(withCountry))
        .anyMatch(l -> "Location".equals(l.getLabel())
            && l.getValue().contains("Berlin") && l.getValue().contains("Germany"));

    OnboardingRequest cityOnly = new OnboardingRequest();
    Supplier s2 = new Supplier();
    Address a2 = new Address();
    a2.setCity("Munich");
    s2.setBusinessAddress(a2);
    cityOnly.setSupplier(s2);
    assertThat(OnboardingRequestSummaryBuilder.build(cityOnly))
        .anyMatch(l -> "Location".equals(l.getLabel()) && "Munich".equals(l.getValue()));
  }

  @Test
  void build_withUrgency_includesLabel() {
    OnboardingRequest request = new OnboardingRequest();
    request.setUrgency("HIGH");

    assertThat(OnboardingRequestSummaryBuilder.build(request))
        .anyMatch(l -> "Urgency".equals(l.getLabel()) && "HIGH".equals(l.getValue()));
  }
}
