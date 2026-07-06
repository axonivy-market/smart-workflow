package com.axonivy.utils.smart.workflow.demo.supplier.onboarding.bean;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.OnboardingRequest;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.audit.AuditTrailEntry;

import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
class TestSupplierOnboardingDeclineBean {

  @Test
  void getDeclineEntry_whenAuditTrailHasDeclineEntry_returnsIt() {
    SupplierOnboardingDeclineBean bean = new SupplierOnboardingDeclineBean();

    AuditTrailEntry declineEntry = new AuditTrailEntry();
    declineEntry.setDeclineReasons(List.of("Insufficient financial stability"));

    AuditTrailEntry otherEntry = new AuditTrailEntry();

    OnboardingRequest request = new OnboardingRequest();
    request.setAuditTrail(new ArrayList<>(List.of(otherEntry, declineEntry)));
    bean.request = request;

    assertThat(bean.getDeclineEntry()).isSameAs(declineEntry);
  }

  @Test
  void getDeclineEntry_whenNoDeclineEntry_returnsLastEntry() {
    SupplierOnboardingDeclineBean bean = new SupplierOnboardingDeclineBean();

    AuditTrailEntry first = new AuditTrailEntry();
    first.setAction("Submitted");
    AuditTrailEntry last = new AuditTrailEntry();
    last.setAction("Completed");

    OnboardingRequest request = new OnboardingRequest();
    request.setAuditTrail(new ArrayList<>(List.of(first, last)));
    bean.request = request;

    assertThat(bean.getDeclineEntry()).isSameAs(last);
  }

  @Test
  void getDeclineEntry_returnsNull_whenNoData() {
    SupplierOnboardingDeclineBean emptyTrail = new SupplierOnboardingDeclineBean();
    OnboardingRequest request = new OnboardingRequest();
    request.setAuditTrail(new ArrayList<>());
    emptyTrail.request = request;
    assertThat(emptyTrail.getDeclineEntry()).isNull();

    SupplierOnboardingDeclineBean nullTrail = new SupplierOnboardingDeclineBean();
    nullTrail.request = new OnboardingRequest();
    assertThat(nullTrail.getDeclineEntry()).isNull();

    SupplierOnboardingDeclineBean nullRequest = new SupplierOnboardingDeclineBean();
    assertThat(nullRequest.getDeclineEntry()).isNull();
  }
}
