package com.axonivy.utils.smart.workflow.demo.erp.supplier.bean;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import javax.el.ELContext;
import javax.el.MethodExpression;
import javax.faces.application.Application;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.apache.commons.lang3.StringUtils;
import org.primefaces.event.FileUploadEvent;

import com.axonivy.utils.smart.workflow.demo.erp.assistant.AgentGuidance;
import com.axonivy.utils.smart.workflow.demo.erp.assistant.AssistantChatMessage;
import com.axonivy.utils.smart.workflow.demo.erp.assistant.AssistantUploadSupport;
import com.axonivy.utils.smart.workflow.demo.erp.document.LegalDocument;
import com.axonivy.utils.smart.workflow.demo.erp.document.LegalDocumentObjectType;
import com.axonivy.utils.smart.workflow.demo.erp.document.LegalDocumentType;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.model.Supplier;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.model.SupplierCertification;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.OnboardingRequest;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.OnboardingStatus;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.parser.OnboardingRequestParser;
import com.axonivy.utils.smart.workflow.utils.IdGenerationUtils;

@ManagedBean
@ViewScoped
public class SupplierRegistrationFormBean extends ReadOnlySupplierDetailsBean
    implements AssistantUploadSupport<OnboardingRequest> {

  private static final long serialVersionUID = 1L;

  private Map<LegalDocumentType, Boolean> certChecked;
  private Map<LegalDocumentType, SupplierCertification> certDetails;

  // ── AssistantUploadSupport state ──────────────────────────────────────────
  private String assistantUploadedFileName;
  private String assistantUploadedContent;
  private Boolean assistantAwaitingConfirmation = Boolean.FALSE;
  private String assistantParseFeedback;
  private List<AssistantUploadSupport.UploadedDocumentEntry> uploadedDocuments = new ArrayList<>();
  private String agentUserMessage;
  private List<AssistantChatMessage> agentChatHistory = new ArrayList<>();

  // ── Initialisation ────────────────────────────────────────────────────────

  @Override
  public void init(OnboardingRequest request) {
    super.init(request);
    initCertificationMaps();
  }

  private void initCertificationMaps() {
    certChecked = new EnumMap<>(LegalDocumentType.class);
    certDetails = new EnumMap<>(LegalDocumentType.class);

    for (LegalDocumentType type : LegalDocumentType.certificationValues()) {
      certChecked.put(type, Boolean.FALSE);
      certDetails.put(type, new SupplierCertification(type, null, null, null, false));
    }

    List<SupplierCertification> existing = request.getSupplier().getCertifications();
    if (existing != null) {
      for (SupplierCertification cert : existing) {
        if (cert.getType() != null) {
          certChecked.put(cert.getType(), cert.isUploaded());
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
    if (StringUtils.isBlank(supplier.getSupplierId())) {
      supplier.setSupplierId(IdGenerationUtils.generateRandomId());
    }
    return supplier.getSupplierId();
  }

  // ── DocumentUploader — hooks (keep supplier's ID list in sync) ────────────

  @Override
  public void onDocumentSaved(LegalDocument doc) {
    Supplier supplier = request.getSupplier();
    if (doc.getDocumentType().isCertification()) {
      supplier.getCertificationDocumentIds().add(doc.getDocumentId());
    } else {
      supplier.getRequiredDocumentIds().add(doc.getDocumentId());
    }
  }

  @Override
  public void onDocumentDeleted(LegalDocument doc) {
    Supplier supplier = request.getSupplier();
    if (doc.getDocumentType().isCertification()) {
      supplier.getCertificationDocumentIds().remove(doc.getDocumentId());
    } else {
      supplier.getRequiredDocumentIds().remove(doc.getDocumentId());
    }
  }

  // ── Form actions ──────────────────────────────────────────────────────────

  public void submitForValidation() {
    flushCertificationsToSupplier();
    persistParsedDocuments();
    request.setStatus(OnboardingStatus.SUPPLIER_DATA);
    callLogicClose();
  }

  private void persistParsedDocuments() {
    if (StringUtils.isBlank(getObjectId()) || getUploadedDocuments().isEmpty()) {
      return;
    }
    for (AssistantUploadSupport.UploadedDocumentEntry entry : getUploadedDocuments()) {
      byte[] content = entry.getContent() != null
          ? entry.getContent().getBytes(StandardCharsets.UTF_8)
          : new byte[0];
      LegalDocumentType docType = LegalDocumentType.fromFileName(entry.getFileName());
      saveDocument(LegalDocument.builder()
          .objectId(getObjectId())
          .objectType(getObjectType())
          .documentType(docType)
          .fileName(entry.getFileName())
          .contentType("text/plain")
          .fileContent(content)
          .fileSize(content.length)
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
        cert = new SupplierCertification(type, null, null, null, false);
      }
      cert.setUploaded(hasDoc || isChecked);
      result.add(cert);
    }
    request.getSupplier().setCertifications(result);
  }

  private void callLogicClose() {
    FacesContext fc = FacesContext.getCurrentInstance();
    ELContext el = fc.getELContext();
    Application app = fc.getApplication();
    MethodExpression closeMethod = app.getExpressionFactory()
        .createMethodExpression(el, "#{logic.close}", null,
            new Class<?>[] { OnboardingRequest.class });
    closeMethod.invoke(el, new Object[] { request });
  }

  // ── AssistantUploadSupport — delegates ────────────────────────────────────

  @Override
  public void addUploadedDocument(FileUploadEvent event) {
    AssistantUploadSupport.super.addUploadedDocument(event);
  }

  @Override
  public void uploadAssistantDocument(FileUploadEvent event) {
    AssistantUploadSupport.super.uploadAssistantDocument(event);
  }

  @Override
  public Object confirmAssistantDocumentParse() {
    return AssistantUploadSupport.super.confirmAssistantDocumentParse();
  }

  public void removeUploadedDocument(ActionEvent event) {
    String fileName = (String) event.getComponent().getAttributes().get("fileName");
    uploadedDocuments.removeIf(d -> d.getFileName().equals(fileName));
    if (uploadedDocuments.isEmpty()) {
      setAssistantAwaitingConfirmation(Boolean.FALSE);
      setAssistantParseFeedback(null);
      setAssistantUploadedFileName(null);
    } else {
      setAssistantParseFeedback(uploadedDocuments.size() + " file(s) ready. Click Confirm Parse to apply.");
      setAssistantUploadedFileName(uploadedDocuments.size() + " file(s) queued");
    }
  }

  @Override
  public void sendAgentMessage() {
    AssistantUploadSupport.super.sendAgentMessage();
  }

  @Override
  public void getAgentAnswer() {
    AssistantUploadSupport.super.getAgentAnswer();
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
        new AgentGuidance(
            "What certifications are required?",
            "use the openSearchSearch tool with collection 'supplier-onboarding-demo'"
                + " to look up certification requirements, then summarize which certifications "
                + "(ISO 9001, ISO 14001, ISO 27001, GDPR DPA) are required and how to fill them in"),
        new AgentGuidance(
            "How does risk scoring work?",
            "use the openSearchSearch tool with collection 'supplier-onboarding-demo'"
                + " to look up risk scoring rules, then explain the four components and "
                + "GREEN/YELLOW/RED thresholds"),
        new AgentGuidance(
            "What documents do I need to upload?",
            "use the openSearchSearch tool with collection 'supplier-onboarding-demo'"
                + " to look up document requirements, then list what needs to be uploaded"),
        new AgentGuidance(
            "Is IBAN required?",
            "use the openSearchSearch tool with collection 'supplier-onboarding-demo'"
                + " to look up banking requirements, then confirm that IBAN is mandatory "
                + "and explain the format"),
        new AgentGuidance(
            "What is a VAT ID?",
            "explain what a VAT ID is, give country-specific format examples (e.g. DE123456789 for Germany), "
                + "and clarify that it is optional but recommended for EU suppliers"),
        new AgentGuidance(
            "Can you parse my supplier document?",
            "ask the user to upload one or more .txt or .md files using the upload button, "
                + "then confirm parsing to auto-fill the registration form fields"));
  }

  @Override
  public List<AssistantUploadSupport.UploadedDocumentEntry> getUploadedDocuments() {
    return uploadedDocuments;
  }

  @Override
  public void setUploadedDocuments(List<AssistantUploadSupport.UploadedDocumentEntry> docs) {
    this.uploadedDocuments = docs;
  }

  @Override
  public void setAssistantParsedDraft(OnboardingRequest assistantParsedDraft) {
    // applied immediately by confirmAssistantDocumentParse via applyParsedDraft — no local storage needed
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
