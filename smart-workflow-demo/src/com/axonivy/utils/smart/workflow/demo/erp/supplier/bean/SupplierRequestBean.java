package com.axonivy.utils.smart.workflow.demo.erp.supplier.bean;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.el.ELContext;
import javax.el.MethodExpression;
import javax.faces.application.Application;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.primefaces.event.FileUploadEvent;

import com.axonivy.utils.smart.workflow.demo.erp.assistant.AgentGuidance;
import com.axonivy.utils.smart.workflow.demo.erp.assistant.AssistantChatMessage;
import com.axonivy.utils.smart.workflow.demo.erp.assistant.AssistantUploadSupport;
import com.axonivy.utils.smart.workflow.demo.erp.department.model.Department;
import com.axonivy.utils.smart.workflow.demo.erp.department.repository.DepartmentRepository;
import com.axonivy.utils.smart.workflow.demo.erp.shared.Address;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.model.Supplier;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.Country;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.AuditTrailEntry;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.OnboardingRequest;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.OnboardingStatus;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.Urgency;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.parser.OnboardingRequestParser;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.processor.SupplierOnboardingProcessService;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.repository.SupplierRepository;

import ch.ivyteam.ivy.environment.Ivy;

@ManagedBean
@ViewScoped
public class SupplierRequestBean implements Serializable, AssistantUploadSupport<OnboardingRequest> {

  private static final long serialVersionUID = 1L;

  private OnboardingRequest request;
  private List<Department> departments = new ArrayList<>();
  private List<Country> countries = new ArrayList<>();
  private List<Urgency> urgencies = new ArrayList<>();

  // ── Document upload state ─────────────────────────────────────────────────
  private String assistantUploadedFileName;
  private String assistantUploadedContent;
  private Boolean assistantAwaitingConfirmation = Boolean.FALSE;
  private String assistantParseFeedback;
  private OnboardingRequest assistantParsedDraft;
  private List<AssistantUploadSupport.UploadedDocumentEntry> uploadedDocuments = new ArrayList<>();

  // ── Agent chat state ──────────────────────────────────────────────────────
  private String agentUserMessage;
  private List<AssistantChatMessage> agentChatHistory = new ArrayList<>();

  public void init(OnboardingRequest request) {
    if (this.request == null) {
      resetAssistantState();
    }
    this.request = request;

    if (request.getRequestedBy() == null) {
      request.setRequestedBy(Ivy.session().getSessionUser().getName());
    }
    if (request.getUrgency() == null) {
      request.setUrgency(Urgency.NORMAL.name());
    }
    if (request.getSupplier() == null) {
      Supplier supplier = new Supplier();
      supplier.setBusinessAddress(new Address());
      request.setSupplier(supplier);
    } else if (request.getSupplier().getBusinessAddress() == null) {
      request.getSupplier().setBusinessAddress(new Address());
    }

    departments = DepartmentRepository.getInstance().findAll();
    countries = Arrays.asList(Country.values());
    urgencies = Arrays.asList(Urgency.values());
  }

  public void submit() {
    request.setStatus(OnboardingStatus.DB_CHECK);
    request.setCaseUuid(Ivy.wfCase().uuid());
    AuditTrailEntry requestEntry = SupplierOnboardingProcessService.buildRequestAuditEntry(request);
    request.setAuditTrail(SupplierOnboardingProcessService.ensureAndAdd(request.getAuditTrail(), requestEntry));
  }

  public void saveDraft() {
    request.setStatus(OnboardingStatus.REQUEST);
    Supplier supplier = request.getSupplier();
    var repo = SupplierRepository.getInstance();
    if (supplier.getSupplierId() == null) {
      repo.create(supplier);
    } else {
      repo.update(supplier);
    }
  }

  public void saveDraftAndClose() {
    saveDraft();
    callLogicClose();
  }

  public void cancel() {
    request.setStatus(OnboardingStatus.COMPLETED);
  }

  public void cancelAndClose() {
    cancel();
    callLogicClose();
  }

  public void submitAndClose() {
    submit();
    callLogicClose();
  }

  // ── AssistantUploadSupport — document upload delegates ────────────────────

  @Override
  public void uploadAssistantDocument(FileUploadEvent event) {
    AssistantUploadSupport.super.uploadAssistantDocument(event);
  }

  @Override
  public void addUploadedDocument(FileUploadEvent event) {
    AssistantUploadSupport.super.addUploadedDocument(event);
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
  public List<AssistantUploadSupport.UploadedDocumentEntry> getUploadedDocuments() {
    return uploadedDocuments;
  }

  @Override
  public void setUploadedDocuments(List<AssistantUploadSupport.UploadedDocumentEntry> docs) {
    this.uploadedDocuments = docs;
  }

  // ── AssistantUploadSupport — agent chat delegates ─────────────────────────

  @Override
  public void sendAgentMessage() {
    AssistantUploadSupport.super.sendAgentMessage();
  }

  @Override
  public void getAgentAnswer() {
    AssistantUploadSupport.super.getAgentAnswer();
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
            "What info do I need for a new supplier?",
            "explain the required fields: supplier business name, legal form, VAT ID, "
                + "business address, primary contact, and the department/business purpose"),
        new AgentGuidance(
            "How does the DB check work?",
            "explain that after submitting the request the system searches the supplier "
                + "database for similar entries by name and country using the findSimilarSuppliers tool, "
                + "then presents any matches so the user can decide whether to reuse an existing supplier "
                + "or proceed with a new registration"),
        new AgentGuidance(
            "What is a valid business purpose?",
            "ask the user to describe the procurement purpose, then suggest the most "
                + "relevant department from the available list and explain what constitutes "
                + "a clear business purpose (e.g. product category, service type, cost centre)"),
        new AgentGuidance(
            "Can you parse my supplier document?",
            "ask the user to upload a .txt or .md file using the upload button, "
                + "then confirm parsing to auto-fill the form fields"));
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
  }

  // ── Getters / setters ─────────────────────────────────────────────────────

  private void callLogicClose() {
    FacesContext fc = FacesContext.getCurrentInstance();
    ELContext el = fc.getELContext();
    Application app = fc.getApplication();
    MethodExpression closeMethod = app.getExpressionFactory()
        .createMethodExpression(el, "#{logic.close}", null, new Class<?>[] { OnboardingRequest.class });
    closeMethod.invoke(el, new Object[] { request });
  }

  public String getDisplayRequester() {
    return Ivy.session().getSessionUser().getDisplayName();
  }

  public OnboardingRequest getRequest() {
    return request;
  }

  public Supplier getSupplier() {
    return request != null ? request.getSupplier() : null;
  }

  public List<Department> getDepartments() {
    return departments;
  }

  public List<Country> getCountries() {
    return countries;
  }

  public List<Urgency> getUrgencies() {
    return urgencies;
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
  public void setAssistantParsedDraft(OnboardingRequest assistantParsedDraft) {
    this.assistantParsedDraft = assistantParsedDraft;
  }

  public OnboardingRequest getAssistantParsedDraft() {
    return assistantParsedDraft;
  }

  public boolean isAssistantUploadEnabled() {
    return request != null;
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
