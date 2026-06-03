package com.axonivy.utils.smart.workflow.demo.erp.supplier.processor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.AuditActorType;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.AuditEntryType;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.AuditTrailEntry;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.OnboardingRequest;

public class SupplierOnboardingProcessService {

  private SupplierOnboardingProcessService() {}

  public static AuditTrailEntry buildRequestAuditEntry(OnboardingRequest req) {
    AuditTrailEntry entry = new AuditTrailEntry();
    entry.setTimestamp(Instant.now().toString());
    entry.setActor(requesterName(req));
    entry.setActorType(AuditActorType.USER);
    entry.setEntryType(AuditEntryType.REQUEST_SUBMITTED);
    entry.setAction("Supplier onboarding request submitted");
    entry.setRequestSummaryLines(req != null ? req.buildSummaryLines() : List.of());
    return entry;
  }

  public static <T> List<T> ensureAndAdd(List<T> list, T item) {
    if (list == null) {
      list = new ArrayList<>();
    }
    list.add(item);
    return list;
  }

  private static String requesterName(OnboardingRequest req) {
    if (req != null && req.getRequestedBy() != null && !req.getRequestedBy().isBlank()) {
      return req.getRequestedBy();
    }
    return "Requester";
  }
}
