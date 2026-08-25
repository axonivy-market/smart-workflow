package com.axonivy.utils.smart.workflow.demo.supplier.onboarding.bean;

import java.util.List;
import java.util.Optional;

import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.OnboardingRequest;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.audit.AuditTrailEntry;

import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;

@Named
@ViewScoped
public class SupplierOnboardingDeclineBean extends AbstractSupplierOnboardingReadonlyBean {

  private static final long serialVersionUID = 1L;

  public AuditTrailEntry getDeclineEntry() {
    List<AuditTrailEntry> trail = Optional.ofNullable(request)
        .map(OnboardingRequest::getAuditTrail)
        .orElse(List.of());

    return trail.stream()
        .filter(e -> e.getDeclineReasons() != null && !e.getDeclineReasons().isEmpty())
        .findFirst()
        .orElseGet(() -> trail.isEmpty() ? null : trail.getLast());
  }
}
