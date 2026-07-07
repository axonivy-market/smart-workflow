package com.axonivy.utils.smart.workflow.demo.supplier.onboarding.bean;

import java.util.List;
import java.util.Optional;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.OnboardingRequest;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.audit.AuditTrailEntry;

@ManagedBean
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
