package com.axonivy.utils.smart.workflow.demo.supplier.onboarding.bean;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

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
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.bean.interfaces.SupplierFormSupport;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.enums.OnboardingStatus;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.enums.Urgency;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.helper.SupplierOnboardingGuidance;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.service.OnboardingAuditEntryFactory;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.service.OnboardingRequestParser;
import com.axonivy.utils.smart.workflow.demo.supplier.repository.SupplierRepository;

import ch.ivyteam.ivy.environment.Ivy;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;

@Named
@ViewScoped
public class SupplierRequestBean implements Serializable, AssistantUploadSupport<OnboardingRequest>, LogicCloseSupport,
                                           SupplierFormSupport {

  private static final long serialVersionUID = 1L;

  private static final String AGENT_SUBPROCESS_SIG  = "askSupplierAssistant(String,String,String)";
  private static final String AGENT_RESPONSE_KEY    = "aiResponse";
  private static final String PARSE_SUBPROCESS_SIG  = "parseOnboardingRequest(String,String,java.io.InputStream)";
  private static final String PARSE_RESULT_KEY      = "draft";
  private OnboardingRequest request;
  private List<Department> departments = new ArrayList<>();
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

    departments = DepartmentRepository.getInstance().findAll(Ivy.wfCase().uuid());
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
    return SupplierOnboardingGuidance.forRequest();
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
