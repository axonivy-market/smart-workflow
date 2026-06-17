package com.axonivy.utils.smart.workflow.demo.supplier.onboarding.service;

import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.OnboardingRequest;

class OnboardingRequestUtils {

  private OnboardingRequestUtils() {}

  public static String supplierName(OnboardingRequest req) {
    if (req != null && req.getSupplier() != null
        && req.getSupplier().getBusinessName() != null
        && !req.getSupplier().getBusinessName().isBlank()) {
      return req.getSupplier().getBusinessName();
    }
    return "Supplier";
  }

  public static String requesterName(OnboardingRequest req) {
    if (req != null && req.getRequestedBy() != null && !req.getRequestedBy().isBlank()) {
      return req.getRequestedBy();
    }
    return "Requester";
  }
}
