package com.axonivy.utils.smart.workflow.demo.supplier.onboarding.bean;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import com.axonivy.utils.smart.workflow.demo.assistant.AgentGuidance;
import com.axonivy.utils.smart.workflow.demo.assistant.AssistantChatMessage;
import com.axonivy.utils.smart.workflow.demo.assistant.AssistantUploadSupport;
import com.axonivy.utils.smart.workflow.demo.assistant.UploadedDocumentEntry;
import com.axonivy.utils.smart.workflow.demo.common.Address;
import com.axonivy.utils.smart.workflow.demo.department.Department;
import com.axonivy.utils.smart.workflow.demo.department.DepartmentRepository;
import com.axonivy.utils.smart.workflow.demo.supplier.Supplier;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.OnboardingRequest;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.audit.AuditTrailEntry;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.bean.interfaces.LogicCloseSupport;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.enums.Country;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.enums.OnboardingStatus;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.enums.Urgency;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.service.OnboardingAuditEntryFactory;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.service.OnboardingRequestParser;
import com.axonivy.utils.smart.workflow.demo.supplier.repository.SupplierRepository;

import ch.ivyteam.ivy.environment.Ivy;

@ManagedBean
@ViewScoped
public class SupplierRequestBean implements Serializable, AssistantUploadSupport<OnboardingRequest>, LogicCloseSupport {

  private static final long serialVersionUID = 1L;

  private static final String AGENT_SUBPROCESS_SIG  = "askSupplierAssistant(String,String,String)";
  private static final String AGENT_RESPONSE_KEY    = "aiResponse";
  private static final String PARSE_SUBPROCESS_SIG  = "parseOnboardingRequest(String,java.io.InputStream)";
  private static final String PARSE_RESULT_KEY      = "draft";
  private OnboardingRequest request;
  private List<Department> departments = new ArrayList<>();
  private List<Country> countries = new ArrayList<>();
  private List<Urgency> urgencies = new ArrayList<>();

  private String assistantUploadedFileName;
  private String assistantUploadedContent;
  private Boolean assistantAwaitingConfirmation = Boolean.FALSE;
  private String assistantParseFeedback;
  private OnboardingRequest assistantParsedDraft;
  private List<UploadedDocumentEntry> uploadedDocuments = new ArrayList<>();

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
    countries = List.of(Country.values());
    urgencies = List.of(Urgency.values());
  }

  public void submit() {
    request.setStatus(OnboardingStatus.DB_CHECK);
    request.setCaseUuid(Ivy.wfCase().uuid());
    AuditTrailEntry requestEntry = OnboardingAuditEntryFactory.buildRequestAuditEntry(request);
    if (request.getAuditTrail() == null) request.setAuditTrail(new ArrayList<>());
    request.getAuditTrail().add(requestEntry);
  }

  public void saveDraft() {
    request.setStatus(OnboardingStatus.REQUEST);
    Supplier supplier = request.getSupplier();
    var repo = SupplierRepository.getInstance();
    if (supplier.getSupplierId() == null) {
      repo.create(request.getCaseUuid(), supplier);
    } else {
      repo.update(request.getCaseUuid(), supplier);
    }
  }

  public void saveDraftAndClose() {
    saveDraft();
    callLogicClose(request);
  }

  public void cancel() {
    request.setStatus(OnboardingStatus.COMPLETED);
  }

  public void cancelAndClose() {
    cancel();
    callLogicClose(request);
  }

  public void submitAndClose() {
    submit();
    callLogicClose(request);
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
  public String getAgentSubProcessSignature() {
    return AGENT_SUBPROCESS_SIG;
  }

  @Override
  public String getAgentResponseKey() {
    return AGENT_RESPONSE_KEY;
  }

  @Override
  public List<AgentGuidance> getAgentGuidance() {
    return List.of(
        guidance("What info do I need for a new supplier?",
            "explain the required fields: supplier business name, legal form, VAT ID, business address, primary contact, and the department/business purpose"),
        guidance("How does the DB check work?",
            "explain that after submitting the request the system searches the supplier database for similar entries by name and country using the findSimilarSuppliers tool, then presents any matches so the user can decide whether to reuse an existing supplier or proceed with a new registration"),
        guidance("What is a valid business purpose?",
            "ask the user to describe the procurement purpose, then suggest the most relevant department from the available list and explain what constitutes a clear business purpose (e.g. product category, service type, cost centre)"),
        guidance("Can you parse my supplier document?",
            "ask the user to upload a .txt or .md file using the upload button, then confirm parsing to auto-fill the form fields"));
  }

  private static AgentGuidance guidance(String questionPattern, String instruction) {
    AgentGuidance g = new AgentGuidance();
    g.setQuestionPattern(questionPattern);
    g.setInstruction(instruction);
    return g;
  }

  @Override
  public OnboardingRequest getFormData() {
    return request;
  }

  @Override
  public String getParseSubProcessSignature() {
    return PARSE_SUBPROCESS_SIG;
  }

  @Override
  public String getParsedResultKey() {
    return PARSE_RESULT_KEY;
  }

  @Override
  public void applyParsedDraft(OnboardingRequest parsedDraft) {
    OnboardingRequestParser.applyDraft(request, parsedDraft);
  }

  public String getDisplayRequester() {
    var user = Ivy.session().getSessionUser();
    return user != null ? user.getDisplayName() : "";
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
