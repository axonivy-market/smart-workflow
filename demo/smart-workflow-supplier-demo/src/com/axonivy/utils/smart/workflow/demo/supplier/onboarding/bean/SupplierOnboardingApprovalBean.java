package com.axonivy.utils.smart.workflow.demo.supplier.onboarding.bean;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.axonivy.utils.ai.SupplierOnboardingApproval.SupplierOnboardingApprovalData;
import com.axonivy.utils.smart.workflow.demo.document.CertificationUploader;
import com.axonivy.utils.smart.workflow.demo.document.LegalDocument;
import com.axonivy.utils.smart.workflow.demo.document.RequiredDocumentUploader;
import com.axonivy.utils.smart.workflow.demo.document.enums.LegalDocumentObjectType;
import com.axonivy.utils.smart.workflow.demo.supplier.Supplier;
import com.axonivy.utils.smart.workflow.demo.supplier.agent.SupplierAgentResponse;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.OnboardingRequest;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.ValidationFinding;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.audit.AuditTrailEntry;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.bean.interfaces.AgentResultView;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.bean.interfaces.DocumentDisplaySupport;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.bean.interfaces.LogicCloseSupport;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.bean.interfaces.RiskLevelSupport;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.bean.interfaces.SupplierFormSupport;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.enums.ApprovalDecision;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.enums.ApprovalStage;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.enums.AuditActorType;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.enums.AuditEntryType;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.helper.OnboardingRequestHelper;

import ch.ivyteam.ivy.environment.Ivy;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;

@Named
@ViewScoped
public class SupplierOnboardingApprovalBean
    implements Serializable, CertificationUploader, RequiredDocumentUploader, RiskLevelSupport, DocumentDisplaySupport,
               SupplierFormSupport, LogicCloseSupport {

  private static final long serialVersionUID = 1L;
  private static final String ACTION_FORMAT = "%s approval decision";

  protected OnboardingRequest request;

  private List<LegalDocument> supplierDocuments = new ArrayList<>();
  private String pendingDocumentType;
  private SupplierAgentResponse agentResponse;

  public void init(OnboardingRequest request) {
    if (this.request != null) {
      return;
    }
    this.request = request;
    ensureNestedObjectsExist();
    loadDocuments();
    if (agentResponse == null) {
      agentResponse = AgentResultView.resolveAgentResponse();
    }
  }

  private void ensureNestedObjectsExist() {
    OnboardingRequestHelper.ensureNestedObjectsExist(request);
  }

  @Override
  public void beforeClose() {
    FacesContext ctx = FacesContext.getCurrentInstance();
    SupplierOnboardingApprovalData data = (SupplierOnboardingApprovalData) ctx.getApplication()
        .evaluateExpressionGet(ctx, "#{data}", Object.class);

    data.setApprovalDecision(Objects.requireNonNullElse(data.getApprovalDecision(), ApprovalDecision.APPROVED));
    data.setApprovalActor(Objects.requireNonNullElse(data.getApprovalActor(), Ivy.session().getSessionUser().getName()));
    data.setApprovalAt(Objects.requireNonNullElse(data.getApprovalAt(), Instant.now().toString()));

    data.setAuditEntry(buildApprovalAuditEntry(
        data.getApprovalDecision(), data.getApprovalComment(),
        data.getApprovalActor(), data.getApprovalAt(), data.getApprovalStage()));
  }

  public AuditTrailEntry buildApprovalAuditEntry(ApprovalDecision decision, String comment,
      String actor, String timestamp, ApprovalStage stage) {
        AuditTrailEntry entry = new AuditTrailEntry();
    entry.setTimestamp(Objects.requireNonNullElseGet(timestamp, () -> Instant.now().toString()));
    entry.setActor(actor);
    entry.setActorType(AuditActorType.USER);
    entry.setEntryType(AuditEntryType.APPROVAL);
    entry.setAction(
    String.format(ACTION_FORMAT, stage == null ? "" : stage.name()));
    entry.setTechnicalDetail(null);
    entry.setStage(stage);
    entry.setDecision(decision);
    entry.setComment(comment);
    return entry;
  }

  @Override
  public List<LegalDocument> getSupplierDocuments() {
    return supplierDocuments;
  }

  @Override
  public void setSupplierDocuments(List<LegalDocument> docs) {
    this.supplierDocuments = docs != null ? docs : new ArrayList<>();
  }

  @Override
  public String getPendingDocumentType() {
    return pendingDocumentType;
  }

  @Override
  public void setPendingDocumentType(String pendingDocumentType) {
    this.pendingDocumentType = pendingDocumentType;
  }

  @Override
  public String getObjectId() {
    return request != null && request.getSupplier() != null
        ? request.getSupplier().getSupplierId()
        : null;
  }

  @Override
  public LegalDocumentObjectType getObjectType() {
    return LegalDocumentObjectType.SUPPLIER;
  }

  public OnboardingRequest getRequest() {
    return request;
  }

  public Supplier getSupplier() {
    return request != null ? request.getSupplier() : null;
  }

  public List<ValidationFinding> getPolicyValidationFindings() {
    return Optional.ofNullable(request)
        .map(OnboardingRequest::getPolicyValidationFindings)
        .orElse(List.of());
  }

  @Override
  public SupplierAgentResponse getAgentResponse() {
    return agentResponse;
  }
}
