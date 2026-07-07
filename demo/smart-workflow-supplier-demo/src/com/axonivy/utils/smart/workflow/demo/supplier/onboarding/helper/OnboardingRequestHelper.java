package com.axonivy.utils.smart.workflow.demo.supplier.onboarding.helper;

import java.util.ArrayList;

import com.axonivy.utils.smart.workflow.demo.common.Address;
import com.axonivy.utils.smart.workflow.demo.supplier.Supplier;
import com.axonivy.utils.smart.workflow.demo.supplier.SupplierBanking;
import com.axonivy.utils.smart.workflow.demo.supplier.SupplierContact;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.OnboardingRequest;

public final class OnboardingRequestHelper {

  private OnboardingRequestHelper() {}

  public static void ensureNestedObjectsExist(OnboardingRequest request) {
    if (request.getSupplier() == null) {
      request.setSupplier(new Supplier());
    }
    Supplier s = request.getSupplier();
    if (s.getBusinessAddress() == null) {
      s.setBusinessAddress(new Address());
    }
    if (s.getPrimaryContact() == null) {
      s.setPrimaryContact(new SupplierContact());
    }
    if (s.getBanking() == null) {
      s.setBanking(new SupplierBanking());
    }
    if (s.getCertifications() == null) {
      s.setCertifications(new ArrayList<>());
    }
  }
}
