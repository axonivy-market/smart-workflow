package com.axonivy.utils.smart.workflow.demo.supplier.onboarding.bean;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import com.axonivy.utils.smart.workflow.demo.assistant.AgentGuidance;
import com.axonivy.utils.smart.workflow.demo.assistant.AssistantChatMessage;
import com.axonivy.utils.smart.workflow.demo.assistant.AssistantUploadSupport;
import com.axonivy.utils.smart.workflow.demo.assistant.UploadedDocumentEntry;
import com.axonivy.utils.smart.workflow.demo.document.LegalDocument;
import com.axonivy.utils.smart.workflow.demo.document.LegalDocumentBuilder;
import com.axonivy.utils.smart.workflow.demo.document.enums.LegalDocumentObjectType;
import com.axonivy.utils.smart.workflow.demo.document.enums.LegalDocumentType;
import com.axonivy.utils.smart.workflow.demo.supplier.Supplier;
import com.axonivy.utils.smart.workflow.demo.supplier.SupplierCertification;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.OnboardingRequest;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.bean.interfaces.LogicCloseSupport;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.enums.OnboardingStatus;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.service.OnboardingRequestParser;
import com.axonivy.utils.smart.workflow.utils.IdGenerationUtils;

@ManagedBean
@ViewScoped
public class SupplierRegistrationFormBean extends ReadOnlySupplierDetailsBean
    implements AssistantUploadSupport<OnboardingRequest>, LogicCloseSupport {

  private static final long serialVersionUID = 1L;

  private Map<LegalDocumentType, Boolean> certChecked;
  private Map<LegalDocumentType, SupplierCertification> certDetails;

  // ── AssistantUploadSupport state ──────────────────────────────────────────
  private String assistantUploadedFileName;
  private String assistantUploadedContent;
  private Boolean assistantAwaitingConfirmation = Boolean.FALSE;
  private String assistantParseFeedback;
  private OnboardingRequest assistantParsedDraft;
  private List<UploadedDocumentEntry> uploadedDocuments = new ArrayList<>();
  private String agentUserMessage;
  private List<AssistantChatMessage> agentChatHistory = new ArrayList<>();

  // ── Initialisation ────────────────────────────────────────────────────────

  public void init(OnboardingRequest request) {
    super.init(request);
    initCertificationMaps();
  }

  private void initCertificationMaps() {
    certChecked = new EnumMap<>(LegalDocumentType.class);
    certDetails = new EnumMap<>(LegalDocumentType.class);

    for (LegalDocumentType type : LegalDocumentType.certificationValues()) {
      certChecked.put(type, Boolean.FALSE);
      SupplierCertification cert = new SupplierCertification();
      cert.setType(type);
      cert.setUploaded(Boolean.FALSE);
      certDetails.put(type, cert);
    }

    List<SupplierCertification> existing = request.getSupplier().getCertifications();
    if (existing != null) {
      for (SupplierCertification cert : existing) {
        if (cert.getType() != null) {
          certChecked.put(cert.getType(), Boolean.TRUE.equals(cert.getUploaded()));
          certDetails.put(cert.getType(), cert);
        }
      }
    }
  }

  // ── DocumentUploader — entity context ─────────────────────────────────────

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

  // ── DocumentUploader — hooks (keep supplier's ID list in sync) ────────────

  @Override
  public void onDocumentSaved(LegalDocument doc) {
    Supplier supplier = request.getSupplier();
    if (doc.getDocumentType().isCertification()) {
      if (supplier.getCertificationDocumentIds() == null) supplier.setCertificationDocumentIds(new ArrayList<>());
      supplier.getCertificationDocumentIds().add(doc.getDocumentId());
    } else {
      if (supplier.getRequiredDocumentIds() == null) supplier.setRequiredDocumentIds(new ArrayList<>());
      supplier.getRequiredDocumentIds().add(doc.getDocumentId());
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

  // ── Form actions ──────────────────────────────────────────────────────────

  public void submitForValidation() {
    flushCertificationsToSupplier();
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

  private void flushCertificationsToSupplier() {
    List<SupplierCertification> result = new ArrayList<>();
    for (LegalDocumentType type : LegalDocumentType.certificationValues()) {
      boolean hasDoc = getDocumentForCert(type) != null;
      boolean isChecked = Boolean.TRUE.equals(certChecked.get(type));
      SupplierCertification cert = certDetails.get(type);
      if (cert == null) {
        cert = new SupplierCertification();
        cert.setType(type);
      }
      cert.setUploaded(hasDoc || isChecked);
      result.add(cert);
    }
    request.getSupplier().setCertifications(result);
  }

  // ── AssistantUploadSupport — abstract contract ────────────────────────────

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
    initCertificationMaps();
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
    return List.of(
        guidance("What certifications are required?",
            "use the openSearchSearch tool with collection 'supplier-onboarding-demo'"
                + " to look up certification requirements, then summarize which certifications "
                + "(ISO 9001, ISO 14001, ISO 27001, GDPR DPA) are required and how to fill them in"),
        guidance("How does risk scoring work?",
            "use the openSearchSearch tool with collection 'supplier-onboarding-demo'"
                + " to look up risk scoring rules, then explain the four components and "
                + "GREEN/YELLOW/RED thresholds"),
        guidance("What documents do I need to upload?",
            "use the openSearchSearch tool with collection 'supplier-onboarding-demo'"
                + " to look up document requirements, then list what needs to be uploaded"),
        guidance("Is IBAN required?",
            "use the openSearchSearch tool with collection 'supplier-onboarding-demo'"
                + " to look up banking requirements, then confirm that IBAN is mandatory "
                + "and explain the format"),
        guidance("What is a VAT ID?",
            "explain what a VAT ID is, give country-specific format examples (e.g. DE123456789 for Germany), "
                + "and clarify that it is optional but recommended for EU suppliers"),
        guidance("Can you parse my supplier document?",
            "ask the user to upload one or more .txt or .md files using the upload button, "
                + "then confirm parsing to auto-fill the registration form fields"));
  }

  private static AgentGuidance guidance(String questionPattern, String instruction) {
    AgentGuidance g = new AgentGuidance();
    g.setQuestionPattern(questionPattern);
    g.setInstruction(instruction);
    return g;
  }

  // ── Getters / setters ─────────────────────────────────────────────────────

  public boolean isAssistantUploadEnabled() {
    return request != null;
  }

  public Map<LegalDocumentType, Boolean> getCertChecked() {
    return certChecked;
  }

  public Map<LegalDocumentType, SupplierCertification> getCertDetails() {
    return certDetails;
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
    this.assistantParsedDraft = assistantParsedDraft;
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
