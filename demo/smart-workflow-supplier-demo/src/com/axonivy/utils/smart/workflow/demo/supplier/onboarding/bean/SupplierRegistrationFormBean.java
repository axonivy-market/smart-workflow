package com.axonivy.utils.smart.workflow.demo.supplier.onboarding.bean;

import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

import com.axonivy.utils.smart.workflow.demo.assistant.AgentGuidance;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.helper.SupplierOnboardingGuidance;
import com.axonivy.utils.smart.workflow.demo.assistant.AssistantChatMessage;
import com.axonivy.utils.smart.workflow.demo.assistant.AssistantUploadSupport;
import com.axonivy.utils.smart.workflow.demo.assistant.UploadedDocumentEntry;
import com.axonivy.utils.smart.workflow.demo.document.CertificationUploader;
import com.axonivy.utils.smart.workflow.demo.document.LegalDocument;
import com.axonivy.utils.smart.workflow.demo.document.LegalDocumentBuilder;
import com.axonivy.utils.smart.workflow.demo.document.RequiredDocumentUploader;
import com.axonivy.utils.smart.workflow.demo.document.enums.LegalDocumentObjectType;
import com.axonivy.utils.smart.workflow.demo.document.enums.LegalDocumentType;
import com.axonivy.utils.smart.workflow.demo.supplier.Supplier;
import com.axonivy.utils.smart.workflow.demo.supplier.SupplierCertification;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.OnboardingRequest;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.ValidationFinding;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.bean.interfaces.LogicCloseSupport;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.bean.interfaces.SupplierFormSupport;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.helper.OnboardingRequestHelper;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.enums.OnboardingStatus;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.helper.CertificationHelper;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.helper.DocumentDisplayHelper;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.service.OnboardingRequestParser;
import com.axonivy.utils.smart.workflow.utils.IdGenerationUtils;

@ManagedBean
@ViewScoped
public class SupplierRegistrationFormBean
    implements Serializable, CertificationUploader, RequiredDocumentUploader,
               AssistantUploadSupport<OnboardingRequest>, LogicCloseSupport, SupplierFormSupport {

  private static final long serialVersionUID = 1L;
  private static final String CONTENT_TYPE_OCTET_STREAM = "application/octet-stream";

  protected OnboardingRequest request;

  private List<LegalDocument> supplierDocuments = new ArrayList<>();
  private String pendingDocumentType;

  private final CertificationHelper certHelper = new CertificationHelper();

  private String assistantUploadedFileName;
  private String assistantUploadedContent;
  private Boolean assistantAwaitingConfirmation = Boolean.FALSE;
  private String assistantParseFeedback;
  private List<UploadedDocumentEntry> uploadedDocuments = new ArrayList<>();
  private String agentUserMessage;
  private List<AssistantChatMessage> agentChatHistory = new ArrayList<>();

  public void init(OnboardingRequest request) {
    if (this.request != null) {
      return;
    }
    this.request = request;
    ensureNestedObjectsExist();
    loadDocuments();
    certHelper.init(request.getSupplier().getCertifications());
  }

  private void ensureNestedObjectsExist() {
    OnboardingRequestHelper.ensureNestedObjectsExist(request);
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

  @Override
  public String ensureObjectId() {
    Supplier supplier = request.getSupplier();
    if (supplier.getSupplierId() == null || supplier.getSupplierId().isBlank()) {
      supplier.setSupplierId(IdGenerationUtils.generateRandomId());
    }
    return supplier.getSupplierId();
  }

  @Override
  public void onDocumentSaved(LegalDocument doc) {
    Supplier supplier = request.getSupplier();
    if (doc.getDocumentType().isCertification()) {
      if (supplier.getCertificationDocumentIds() == null) {
        supplier.setCertificationDocumentIds(new ArrayList<>());
      }
      supplier.getCertificationDocumentIds().add(doc.getDocumentId());
    } else {
      if (supplier.getRequiredDocumentIds() == null) {
        supplier.setRequiredDocumentIds(new ArrayList<>());
      }

      if (!supplier.getRequiredDocumentIds().contains(doc.getDocumentId())) {
        supplier.getRequiredDocumentIds().add(doc.getDocumentId());
      }
    }
  }

  @Override
  public void onDocumentDeleted(LegalDocument doc) {
    Supplier supplier = request.getSupplier();
    if (doc.getDocumentType().isCertification()) {
      if (supplier.getCertificationDocumentIds() != null) {
        supplier.getCertificationDocumentIds().remove(doc.getDocumentId());
      }
    } else {
      if (supplier.getRequiredDocumentIds() != null) {
        supplier.getRequiredDocumentIds().remove(doc.getDocumentId());
      }
    }
  }

  public void submitForValidation() {
    request.getSupplier().setCertifications(
        certHelper.buildCertificationList(this::getDocumentForCert));
    persistParsedDocuments();
    request.setStatus(OnboardingStatus.SUPPLIER_DATA);
    callLogicClose(request);
  }

  private void persistParsedDocuments() {
    if (getObjectId() == null || getObjectId().isBlank() || getUploadedDocuments().isEmpty()) {
      return;
    }
    for (UploadedDocumentEntry entry : getUploadedDocuments()) {
      byte[] content = entry.getData() != null ? entry.getData() : new byte[0];
      LegalDocumentType docType = LegalDocumentType.fromFileName(entry.getFileName());
      saveDocument(LegalDocumentBuilder.builder()
          .objectId(getObjectId())
          .objectType(getObjectType())
          .documentType(docType)
          .fileName(entry.getFileName())
          .fileContent(content)
          .uploadedNow()
          .build());
    }
  }

  @Override
  public LegalDocument getDocumentByTypeKey(String typeKey) {
    if (typeKey == null) {
      return null;
    }
    if (typeKey.startsWith(DocumentDisplayHelper.CERT_PREFIX)) {
      String certName = typeKey.substring(DocumentDisplayHelper.CERT_PREFIX.length());
      try {
        return getDocumentByType(LegalDocumentType.valueOf(certName));
      } catch (IllegalArgumentException e) {
        return null;
      }
    }
    if (typeKey.startsWith(DocumentDisplayHelper.DOC_PREFIX)) {
      String docName = typeKey.substring(DocumentDisplayHelper.DOC_PREFIX.length());
      return getSupplierDocuments().stream()
          .filter(doc -> docName.equalsIgnoreCase(doc.getDescription()))
          .findFirst().orElse(null);
    }
    try {
      return getDocumentByType(LegalDocumentType.valueOf(typeKey));
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  @Override
  public StreamedContent downloadDocument(String documentId) {
    LegalDocument doc = getSupplierDocuments().stream()
        .filter(candidate -> documentId.equals(candidate.getDocumentId()))
        .findFirst().orElse(null);
    if (doc == null || doc.getFileContent() == null) {
      return null;
    }
    byte[] content = doc.getFileContent();
    String fileName = doc.getFileName();
    return DefaultStreamedContent.builder()
        .name(fileName)
        .contentType(CONTENT_TYPE_OCTET_STREAM)
        .stream(() -> new ByteArrayInputStream(content))
        .build();
  }

  @Override
  public LegalDocument getCompanyRegistrationDoc() {
    return getDocumentByType(LegalDocumentType.COMMERCIAL_REGISTER);
  }

  @Override
  public LegalDocument getSelfDeclarationDoc() {
    return getDocumentByType(LegalDocumentType.SELF_DECLARATION);
  }

  @Override
  public LegalDocument getAnnualReportDoc() {
    return getDocumentByType(LegalDocumentType.ANNUAL_REPORT);
  }

  public String getDocumentTypeLabel(String typeKey) {
    return DocumentDisplayHelper.getDocumentTypeLabel(typeKey);
  }

  public String getDocumentTypeSubtitle(String typeKey) {
    return DocumentDisplayHelper.getDocumentTypeSubtitle(typeKey);
  }

  public LegalDocumentType[] getCertificationTypes() {
    return DocumentDisplayHelper.getCertificationTypes();
  }

  public boolean isLegalDocumentTypeRequired(String typeKey) {
    return DocumentDisplayHelper.isLegalDocumentTypeRequired(typeKey);
  }

  public Map<String, Boolean> getLegalDocumentTypeRequired() {
    return DocumentDisplayHelper.getLegalDocumentTypeRequired();
  }

  public String getScoreWidthClass(int score) {
    return DocumentDisplayHelper.getScoreWidthClass(score);
  }

  @Override
  public OnboardingRequest getFormData() {
    return request;
  }

  @Override
  public String getParseSubProcessSignature() {
    return "parseOnboardingRequest(String,java.io.InputStream)";
  }

  @Override
  public String getParsedResultKey() {
    return "draft";
  }

  @Override
  public void applyParsedDraft(OnboardingRequest parsedDraft) {
    OnboardingRequestParser.applyDraft(request, parsedDraft);
    certHelper.init(request.getSupplier().getCertifications());
  }

  @Override
  public String getAgentSubProcessSignature() {
    return "askSupplierAssistant(String,String,String)";
  }

  @Override
  public String getAgentResponseKey() {
    return "aiResponse";
  }

  @Override
  public List<AgentGuidance> getAgentGuidance() {
    return SupplierOnboardingGuidance.forRegistration();
  }

  public OnboardingRequest getRequest() {
    return request;
  }

  public List<ValidationFinding> getPolicyValidationFindings() {
    if (request == null || request.getPolicyValidationFindings() == null) {
      return Collections.emptyList();
    }
    return request.getPolicyValidationFindings();
  }

  public Supplier getSupplier() {
    return request != null ? request.getSupplier() : null;
  }

  public boolean isAssistantUploadEnabled() {
    return request != null;
  }

  public Map<LegalDocumentType, Boolean> getCertChecked() {
    return certHelper.getCertChecked();
  }

  public Map<LegalDocumentType, SupplierCertification> getCertDetails() {
    return certHelper.getCertDetails();
  }

  @Override
  public List<UploadedDocumentEntry> getUploadedDocuments() {
    return uploadedDocuments;
  }

  @Override
  public void setUploadedDocuments(List<UploadedDocumentEntry> docs) {
    this.uploadedDocuments = docs;
  }

  @Override
  public void setAssistantParsedDraft(OnboardingRequest assistantParsedDraft) {
  }

  public String getAssistantUploadedFileName() {
    return assistantUploadedFileName;
  }

  @Override
  public void setAssistantUploadedFileName(String assistantUploadedFileName) {
    this.assistantUploadedFileName = assistantUploadedFileName;
  }

  @Override
  public String getAssistantUploadedContent() {
    return assistantUploadedContent;
  }

  @Override
  public void setAssistantUploadedContent(String assistantUploadedContent) {
    this.assistantUploadedContent = assistantUploadedContent;
  }

  public Boolean getAssistantAwaitingConfirmation() {
    return assistantAwaitingConfirmation;
  }

  @Override
  public void setAssistantAwaitingConfirmation(Boolean assistantAwaitingConfirmation) {
    this.assistantAwaitingConfirmation = assistantAwaitingConfirmation;
  }

  public String getAssistantParseFeedback() {
    return assistantParseFeedback;
  }

  @Override
  public void setAssistantParseFeedback(String assistantParseFeedback) {
    this.assistantParseFeedback = assistantParseFeedback;
  }

  @Override
  public String getAgentUserMessage() {
    return agentUserMessage;
  }

  @Override
  public void setAgentUserMessage(String agentUserMessage) {
    this.agentUserMessage = agentUserMessage;
  }

  @Override
  public List<AssistantChatMessage> getAgentChatHistory() {
    return agentChatHistory;
  }

  @Override
  public void setAgentChatHistory(List<AssistantChatMessage> agentChatHistory) {
    this.agentChatHistory = agentChatHistory;
  }
}
