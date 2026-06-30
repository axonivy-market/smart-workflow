package com.axonivy.utils.smart.workflow.demo.supplier.onboarding.bean;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import com.axonivy.utils.smart.workflow.demo.document.CertificationUploader;
import com.axonivy.utils.smart.workflow.demo.document.LegalDocument;
import com.axonivy.utils.smart.workflow.demo.document.RequiredDocumentUploader;
import com.axonivy.utils.smart.workflow.demo.document.enums.LegalDocumentObjectType;
import com.axonivy.utils.smart.workflow.demo.supplier.Supplier;
import com.axonivy.utils.smart.workflow.demo.supplier.agent.SupplierAgentResponse;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.OnboardingRequest;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.SupplierRiskScore;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.ValidationFinding;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.bean.interfaces.AgentResultView;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.bean.interfaces.DocumentDisplaySupport;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.bean.interfaces.RiskLevelSupport;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.bean.interfaces.SupplierFormSupport;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.enums.FindingSeverity;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.helper.OnboardingRequestHelper;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.helper.RiskScoreHelper;

import ch.ivyteam.ivy.environment.Ivy;

@ManagedBean
@ViewScoped
public class SupplierClarificationBean
    implements Serializable, CertificationUploader, RequiredDocumentUploader, RiskLevelSupport, DocumentDisplaySupport,
               SupplierFormSupport {

  private static final long serialVersionUID = 1L;

  protected OnboardingRequest request;

  private List<LegalDocument> supplierDocuments = new ArrayList<>();
  private String pendingDocumentType;
  private SupplierAgentResponse agentResponse;

  private int originalFinancialStability;
  private int originalPolicyCompliance;
  private int originalCertValidity;

  private final List<ValidationFinding> clarificationFindings = new ArrayList<>();

  private int expandedItemIndex = -1;

  // --- Lifecycle ---

  public void init(OnboardingRequest request) {
    if (this.request != null) {
      return;
    }
    this.request = request;
    ensureNestedObjectsExist();
    loadDocuments();
    if (agentResponse == null) {
      agentResponse = AgentResultView.resolveAgentResponse();
      captureOriginalScores();
    }
    if (clarificationFindings.isEmpty()) {
      clarificationFindings.addAll(filterNonPassedFindings(request.getPolicyValidationFindings()));
    }
  }

  // --- Actions ---

  public void toggleResolve(int index) {
    if (expandedItemIndex == index) {
      expandedItemIndex = -1;
    } else {
      expandedItemIndex = index;
      if (index >= 0 && index < clarificationFindings.size()) {
        ValidationFinding finding = clarificationFindings.get(index);
        if (finding.getDocumentTypeKey() != null) {
          setPendingDocumentType(finding.getDocumentTypeKey());
        }
      }
    }
  }

  public void markItemResolved(int index) {
    if (index >= 0 && index < clarificationFindings.size()) {
      clarificationFindings.get(index).setResolved(true);
      recalculateScore();
    }
    expandedItemIndex = -1;
  }

  // --- State accessors ---

  public OnboardingRequest getRequest() {
    return request;
  }

  public Supplier getSupplier() {
    return request != null ? request.getSupplier() : null;
  }

  public List<ValidationFinding> getPolicyValidationFindings() {
    if (request == null || request.getPolicyValidationFindings() == null) {
      return java.util.Collections.emptyList();
    }
    return request.getPolicyValidationFindings();
  }

  @Override
  public SupplierAgentResponse getAgentResponse() {
    return agentResponse;
  }

  public String getBannerBadgeLabel() {
    return switch (getRiskLevel()) {
      case GREEN -> Ivy.cms().co(CMS_COMPLETION    + "RiskScoreBadge");
      case RED   -> Ivy.cms().co(CMS_DECLINE       + "RiskScoreBadge");
      default    -> Ivy.cms().co(CMS_CLARIFICATION + "RiskScoreBadge");
    };
  }

  public boolean isAllItemsResolved() {
    return !clarificationFindings.isEmpty()
        && clarificationFindings.stream().allMatch(f -> Boolean.TRUE.equals(f.getResolved()));
  }

  public ValidationFinding[] getValidationFindingsArray() {
    return clarificationFindings.toArray(ValidationFinding[]::new);
  }

  public boolean isItemExpanded(int index) {
    return expandedItemIndex == index;
  }

  public int getExpandedItemIndex() {
    return expandedItemIndex;
  }

  public void setExpandedItemIndex(int expandedItemIndex) {
    this.expandedItemIndex = expandedItemIndex;
  }

  // --- Interface implementations (DocumentUploader) ---

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

  @Override
  public void onDocumentSaved(LegalDocument doc) {
    RequiredDocumentUploader.super.onDocumentSaved(doc);
    if (expandedItemIndex >= 0 && expandedItemIndex < clarificationFindings.size()) {
      clarificationFindings.get(expandedItemIndex).setResolved(true);
    }
  }

  // --- Private helpers ---

  private void ensureNestedObjectsExist() {
    OnboardingRequestHelper.ensureNestedObjectsExist(request);
  }

  private void captureOriginalScores() {
    if (Optional.ofNullable(agentResponse).map(SupplierAgentResponse::getRiskScore).isEmpty()) {
      return;
    }
    SupplierRiskScore rs = agentResponse.getRiskScore();
    originalFinancialStability = Optional.ofNullable(rs.getFinancialStability()).orElse(0);
    originalPolicyCompliance   = Optional.ofNullable(rs.getPolicyCompliance()).orElse(0);
    originalCertValidity       = Optional.ofNullable(rs.getCertValidity()).orElse(0);
  }

  private static List<ValidationFinding> filterNonPassedFindings(List<ValidationFinding> findings) {
    if (findings == null) {
      return List.of();
    }
    List<ValidationFinding> result = new ArrayList<>();
    for (ValidationFinding f : findings) {
      if (f.getSeverity() != FindingSeverity.PASSED) {
        result.add(f);
      }
    }
    return result;
  }

  private void recalculateScore() {
    if (agentResponse == null || agentResponse.getRiskScore() == null) {
      return;
    }
    RiskScoreHelper.recalculateScore(
        agentResponse.getRiskScore(), clarificationFindings,
        originalFinancialStability, originalPolicyCompliance, originalCertValidity);
  }
}
