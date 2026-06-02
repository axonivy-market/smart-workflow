package com.axonivy.utils.smart.workflow.demo.erp.procurement.bean;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.file.UploadedFile;

import com.axonivy.utils.smart.workflow.demo.erp.assistant.AgentGuidance;
import com.axonivy.utils.smart.workflow.demo.erp.assistant.AssistantChatMessage;
import com.axonivy.utils.smart.workflow.demo.erp.procurement.agent.ProcurementAgentResponse;
import com.axonivy.utils.smart.workflow.demo.erp.procurement.agent.feedback.AgentFeedback;
import com.axonivy.utils.smart.workflow.demo.erp.procurement.agent.feedback.FeedbackType;
import com.axonivy.utils.smart.workflow.demo.erp.procurement.model.ProcurementItem;
import com.axonivy.utils.smart.workflow.demo.erp.procurement.model.ProcurementRequest;
import com.axonivy.utils.smart.workflow.demo.erp.procurement.model.RequestPriority;
import com.axonivy.utils.smart.workflow.demo.erp.procurement.repository.ProcurementItemRepository;
import com.axonivy.utils.smart.workflow.demo.utils.IvyAdapterService;

import ch.ivyteam.ivy.environment.Ivy;

@ManagedBean
@ViewScoped
public class ProcurementRequestBean implements Serializable {

  private static final long serialVersionUID = 1L;

  private ProcurementRequest request;

  private List<ProcurementItem> availableItems = new ArrayList<>();

  private static final List<String> UNIT_OPTIONS = Arrays.asList(
      "pcs", "kg", "t", "m", "m²", "m³", "l", "bag", "pallet", "roll");

  // ── Document upload state ─────────────────────────────────────────────────
  private String assistantUploadedFileName;
  private Boolean assistantAwaitingConfirmation = Boolean.FALSE;
  private String assistantParseFeedback;
  private List<UploadedDocumentEntry> uploadedDocuments = new ArrayList<>();

  // ── Agent chat state ──────────────────────────────────────────────────────
  private String agentUserMessage;
  private List<AssistantChatMessage> agentChatHistory = new ArrayList<>();

  /**
   * Simple entry to queue uploaded documents for multi-document parsing.
   */
  public static final class UploadedDocumentEntry implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String fileName;
    private final byte[] data;

    public UploadedDocumentEntry(String fileName, byte[] data) {
      this.fileName = fileName;
      this.data = data;
    }

    public String getFileName() { return fileName; }

    public InputStream getInputStream() {
      return new ByteArrayInputStream(data);
    }

    public String getContent() {
      return new String(data, StandardCharsets.UTF_8);
    }

    public boolean isPdf() {
      return fileName != null && fileName.toLowerCase(Locale.ROOT).endsWith(".pdf");
    }
  }

  /**
   * Called from XHTML preRender event.
   * Initializes a new request with one empty material row.
   */
  public void preRender(ProcurementRequest existingRequest, String requesterName) {
    availableItems = ProcurementItemRepository.getInstance().findAll();

    if (request != null) {
      return; // already initialized — ViewScoped guard
    }
    if (existingRequest != null) {
      this.request = existingRequest;
    } else {
      this.request = new ProcurementRequest();
      this.request.setRequester(requesterName != null ? requesterName : "");
      addRow();
    }
    recalculate();
    resetAssistantState();
  }

  public void addRow() {
    ProcurementItem item = new ProcurementItem();
    item.setPosition(request.getMaterialItems().size() + 1);
    request.getMaterialItems().add(item);
  }

  public void removeRow(int index) {
    if (index >= 0 && index < request.getMaterialItems().size()) {
      request.getMaterialItems().remove(index);
      request.reindexItems();
      recalculate();
    }
  }

  public void recalculate() {
    request.calculateTotalNetAmount();
  }

  public boolean validate() {
    boolean valid = true;
    FacesContext ctx = FacesContext.getCurrentInstance();

    if (request.getProjectName() == null || request.getProjectName().trim().isEmpty()) {
      ctx.addMessage("form:project-name", new FacesMessage(FacesMessage.SEVERITY_ERROR,
          Ivy.cms().co("/Dialogs/procurement/request/CreateRequestDialog/ProjectNameRequired"), null));
      valid = false;
    }
    if (request.getProjectNumberCostCenter() == null || request.getProjectNumberCostCenter().trim().isEmpty()) {
      ctx.addMessage("form:project-number", new FacesMessage(FacesMessage.SEVERITY_ERROR,
          Ivy.cms().co("/Dialogs/procurement/request/CreateRequestDialog/ProjectNumberRequired"), null));
      valid = false;
    }
    if (request.getConstructionSiteDeliveryAddress() == null || request.getConstructionSiteDeliveryAddress().trim().isEmpty()) {
      ctx.addMessage("form:delivery-address", new FacesMessage(FacesMessage.SEVERITY_ERROR,
          Ivy.cms().co("/Dialogs/procurement/request/CreateRequestDialog/DeliveryAddressRequired"), null));
      valid = false;
    }
    if (request.getRequiredDeliveryDate() == null || request.getRequiredDeliveryDate().isBlank()) {
      ctx.addMessage("form:delivery-date", new FacesMessage(FacesMessage.SEVERITY_ERROR,
          Ivy.cms().co("/Dialogs/procurement/request/CreateRequestDialog/DeliveryDateRequired"), null));
      valid = false;
    }
    if (request.getMaterialItems().isEmpty()) {
      ctx.addMessage("form:material-table", new FacesMessage(FacesMessage.SEVERITY_ERROR,
          Ivy.cms().co("/Dialogs/procurement/request/CreateRequestDialog/MaterialItemRequired"), null));
      valid = false;
    }

    for (int i = 0; i < request.getMaterialItems().size(); i++) {
      ProcurementItem item = request.getMaterialItems().get(i);
      if (item.getMaterialDescription() == null || item.getMaterialDescription().trim().isEmpty()) {
        ctx.addMessage("form:material-table", new FacesMessage(FacesMessage.SEVERITY_ERROR,
            Ivy.cms().co("/Dialogs/procurement/request/CreateRequestDialog/RowMaterialDescRequired", Arrays.asList(i + 1)), null));
        valid = false;
      }
      if (item.getPurpose() == null || item.getPurpose().trim().isEmpty()) {
        ctx.addMessage("form:material-table", new FacesMessage(FacesMessage.SEVERITY_ERROR,
            Ivy.cms().co("/Dialogs/procurement/request/CreateRequestDialog/RowPurposeRequired", Arrays.asList(i + 1)), null));
        valid = false;
      }
    }

    return valid;
  }

  // ── Document upload ───────────────────────────────────────────────────────

  public void addUploadedDocument(FileUploadEvent event) {
    UploadedFile uploadedFile = event != null ? event.getFile() : null;
    if (uploadedFile == null) {
      assistantParseFeedback = Ivy.cms().co("/Dialogs/procurement/request/CreateRequestDialog/NoFileUploaded");
      return;
    }

    String fileName = uploadedFile.getFileName();
    if (fileName == null || fileName.trim().isEmpty()) {
      assistantParseFeedback = Ivy.cms().co("/Dialogs/procurement/request/CreateRequestDialog/NoFileUploaded");
      return;
    }

    String lowerName = fileName.toLowerCase(Locale.ROOT);
    if (!(lowerName.endsWith(".txt") || lowerName.endsWith(".md") || lowerName.endsWith(".pdf"))) {
      assistantParseFeedback = Ivy.cms().co("/Dialogs/procurement/request/components/AiAssistant/InvalidFileMessage");
      return;
    }

    byte[] data = uploadedFile.getContent();
    if (data == null || data.length == 0) {
      assistantParseFeedback = Ivy.cms().co("/Dialogs/procurement/request/CreateRequestDialog/UploadedFileEmpty");
      return;
    }

    List<UploadedDocumentEntry> updated = new ArrayList<>(uploadedDocuments);
    updated.add(new UploadedDocumentEntry(fileName.trim(), data));
    uploadedDocuments = updated;
    assistantUploadedFileName = Ivy.cms().co("/Dialogs/procurement/request/CreateRequestDialog/FilesQueued", Arrays.asList(updated.size()));
    assistantAwaitingConfirmation = Boolean.TRUE;
    assistantParseFeedback = Ivy.cms().co("/Dialogs/procurement/request/CreateRequestDialog/FilesReadyToParse", Arrays.asList(updated.size()));
  }

  public void removeUploadedDocument(ActionEvent event) {
    String fileName = (String) event.getComponent().getAttributes().get("fileName");
    uploadedDocuments.removeIf(d -> d.getFileName().equals(fileName));
    if (uploadedDocuments.isEmpty()) {
      assistantAwaitingConfirmation = Boolean.FALSE;
      assistantParseFeedback = null;
      assistantUploadedFileName = null;
    } else {
      assistantParseFeedback = Ivy.cms().co("/Dialogs/procurement/request/CreateRequestDialog/FilesReadyToParse", Arrays.asList(uploadedDocuments.size()));
      assistantUploadedFileName = Ivy.cms().co("/Dialogs/procurement/request/CreateRequestDialog/FilesQueued", Arrays.asList(uploadedDocuments.size()));
    }
  }

  public Object confirmAssistantDocumentParse() {
    if (uploadedDocuments == null || uploadedDocuments.isEmpty()) {
      assistantAwaitingConfirmation = Boolean.FALSE;
      assistantParseFeedback = Ivy.cms().co("/Dialogs/procurement/request/CreateRequestDialog/UploadFileFirstMessage");
      return null;
    }

    UploadedDocumentEntry pdfDoc = uploadedDocuments.stream()
        .filter(UploadedDocumentEntry::isPdf)
        .findFirst()
        .orElse(null);

    try {
      Map<String, Object> params = new HashMap<>();
      if (pdfDoc != null) {
        params.put("inputStream", pdfDoc.getInputStream());
      } else {
        String content = buildCombinedContent();
        if (content == null || content.isBlank()) {
          assistantAwaitingConfirmation = Boolean.FALSE;
          assistantParseFeedback = Ivy.cms().co("/Dialogs/procurement/request/CreateRequestDialog/UploadFileFirstMessage");
          return null;
        }
        params.put("content", content);
      }

      Map<String, Object> result = IvyAdapterService.startSubProcessInSecurityContext(
          "parseProcurementDocument(String,java.io.InputStream)", params);

      ProcurementRequest parsedDraft = result != null
          ? (ProcurementRequest) result.get("parsedDraft") : null;

      if (parsedDraft == null) {
        assistantAwaitingConfirmation = Boolean.TRUE;
        assistantParseFeedback = getFeedbackOrDefault(result,
            Ivy.cms().co("/Dialogs/procurement/request/CreateRequestDialog/ParseNoValues"));
        return null;
      }

      applyParsedDraft(parsedDraft);
      assistantAwaitingConfirmation = Boolean.FALSE;
      String parseMessage = getFeedbackOrDefault(result,
          Ivy.cms().co("/Dialogs/procurement/request/CreateRequestDialog/ParseFormUpdated"));
      addAssistantMessage(parseMessage);
      assistantParseFeedback = null;
      uploadedDocuments = new ArrayList<>();
      assistantUploadedFileName = null;
    } catch (Exception ex) {
      assistantAwaitingConfirmation = Boolean.TRUE;
      assistantParseFeedback = Ivy.cms().co("/Dialogs/procurement/request/CreateRequestDialog/ParseFailedMessage", Arrays.asList(ex.getMessage()));
      Ivy.log().warn("Assistant parse failed", ex);
    }

    return null;
  }

  private void applyParsedDraft(ProcurementRequest draft) {
    if (draft == null) {
      return;
    }
    if (draft.getProjectName() != null) {
      request.setProjectName(draft.getProjectName());
    }
    if (draft.getProjectNumberCostCenter() != null) {
      request.setProjectNumberCostCenter(draft.getProjectNumberCostCenter());
    }
    if (draft.getConstructionSiteDeliveryAddress() != null) {
      request.setConstructionSiteDeliveryAddress(draft.getConstructionSiteDeliveryAddress());
    }
    if (draft.getRequiredDeliveryDate() != null) {
      request.setRequiredDeliveryDate(draft.getRequiredDeliveryDate());
    }
    if (draft.getPriority() != null) {
      request.setPriority(draft.getPriority());
    }
    if (draft.getOrderNotes() != null) {
      request.setOrderNotes(draft.getOrderNotes());
    }
    if (draft.getMaterialItems() != null && !draft.getMaterialItems().isEmpty()) {
      for (ProcurementItem item : draft.getMaterialItems()) {
        if (item.getMaterialTypeId() == null && item.getMaterialDescription() != null) {
          List<ProcurementItem> matches = ProcurementItemRepository.getInstance()
              .searchByDescription(item.getMaterialDescription());
          if (!matches.isEmpty()) {
            item.setMaterialTypeId(matches.get(0).getMaterialTypeId());
          }
        }
      }
      request.setMaterialItems(draft.getMaterialItems());
      request.reindexItems();
    }
    recalculate();
  }

  // ── Agent chat ────────────────────────────────────────────────────────────

  public void sendAgentMessage() {
    if (agentUserMessage == null || agentUserMessage.isBlank()) {
      return;
    }
    if (request != null && request.getMaterialItems() != null) {
      for (ProcurementItem item : request.getMaterialItems()) {
        item.setChanged(false);
      }
    }
    List<AssistantChatMessage> history = new ArrayList<>(agentChatHistory);
    history.add(new AssistantChatMessage("user", agentUserMessage.trim()));
    agentChatHistory = history;
    agentUserMessage = "";
  }

  public void getAgentAnswer() {
    try {
      String latestQuestion = "";
      List<String> chatHistory = new ArrayList<>();

      if (!agentChatHistory.isEmpty()) {
        AssistantChatMessage last = agentChatHistory.get(agentChatHistory.size() - 1);
        latestQuestion = last.getContent();
        List<AssistantChatMessage> previousTurns = agentChatHistory.subList(0, agentChatHistory.size() - 1);
        if (!previousTurns.isEmpty()) {
          int fromIndex = Math.max(0, previousTurns.size() - 10);
          for (AssistantChatMessage msg : previousTurns.subList(fromIndex, previousTurns.size())) {
            chatHistory.add(msg.getRole() + ": " + msg.getContent());
          }
        }
      }

      Map<String, Object> params = new HashMap<>();
      params.put("query", latestQuestion);
      params.put("chatHistory", chatHistory);
      params.put("request", request);

      Map<String, Object> result = IvyAdapterService.startSubProcessInSecurityContext(
          "invokeAssistantAgent(String,java.util.List<String>,com.axonivy.utils.smart.workflow.demo.erp.procurement.model.ProcurementRequest)", params);

      ProcurementAgentResponse agentResponse = (result != null)
          ? (ProcurementAgentResponse) result.get("agentResponse")
          : null;

      var feedbackList = Optional.ofNullable(agentResponse).map(ProcurementAgentResponse::getFeedbackList).orElse(List.of());
      for (AgentFeedback feedback : feedbackList) {
        if (feedback.getType() == FeedbackType.EVALUATION && !feedback.isSuccess()) {
          request.getMaterialItems().stream()
              .filter(item -> item.getId().equals(feedback.getId()))
              .findFirst()
              .ifPresent(item -> item.setHasTrouble(true));
        }

        if (feedback.getType() == FeedbackType.OPTION && feedback.getFeedbackOption() != null) {
          request.getMaterialItems().stream()
              .filter(item -> item.getId().equals(feedback.getId()))
              .findFirst()
              .ifPresent(item -> item.setFeedbackOption(feedback.getFeedbackOption()));
        }
      }

      Optional.ofNullable(agentResponse)
          .map(ProcurementAgentResponse::getRequest)
          .ifPresent(updatedRequest -> applyParsedDraft(updatedRequest));

      addAssistantMessage(agentResponse);
    } catch (Exception e) {
      List<AssistantChatMessage> updated = new ArrayList<>(agentChatHistory);
      updated.add(new AssistantChatMessage("assistant", Ivy.cms().co("/Dialogs/procurement/request/CreateRequestDialog/AgentErrorMessage", Arrays.asList(e.getMessage()))));
      agentChatHistory = updated;
      Ivy.log().warn("Agent chat failed", e);
    }
  }

  private void addAssistantMessage(ProcurementAgentResponse response) {
    String text = (response != null && response.getResult() != null)
        ? response.getResult()
        : Ivy.cms().co("/Dialogs/procurement/request/CreateRequestDialog/AgentNoResponseMessage");
    List<AgentFeedback> feedbackList = (response != null) ? response.getFeedbackList() : null;
    List<AssistantChatMessage> updated = new ArrayList<>(agentChatHistory);
    updated.add(new AssistantChatMessage("assistant", text, feedbackList));
    agentChatHistory = updated;
  }

  private void addAssistantMessage(String text) {
    List<AssistantChatMessage> updated = new ArrayList<>(agentChatHistory);
    updated.add(new AssistantChatMessage("assistant", text));
    agentChatHistory = updated;
  }

  public List<AgentGuidance> getAgentGuidance() {
    return List.of(
        new AgentGuidance(
            "Parse my document",
            "ask the user to upload a .txt, .md, or .pdf file using the upload button, "
                + "then confirm parsing to auto-fill the form fields"),
        new AgentGuidance(
            "Analyze my request",
            "analyze the current procurement request against the inventory and check whether "
                + "each requested item can be fulfilled from stock. "
                + "For each material item, report the item name, requested quantity, available stock, "
                + "and whether it can be fulfilled in full, partially, or not at all. "
                + "End with a short summary and any recommendations."),
        new AgentGuidance(
            "How do I add materials?",
            "explain step by step how to add material items to the request: "
                + "click 'Add Row' to create a new line, then expand the row using the toggle arrow to fill in "
                + "material description, standard/grade, quantity, unit, unit price, supplier, purpose, and comment. "
                + "The total net amount is calculated automatically. "
                + "At least one item with a description and purpose is required before submitting."),
        new AgentGuidance(
            "What happens after I submit?",
            "explain the approval workflow that follows submission: "
                + "the request is forwarded to the purchasing department for review, "
                + "a purchasing officer checks the material items against stock and supplier availability, "
                + "high-value or urgent requests may require manager approval, "
                + "and the requester is notified of the outcome. "
                + "Describe what URGENT and HIGH priority means for processing time."));
  }

  public List<String> getAvailableMaterialDescriptions() {
    if (availableItems == null) {
      return new ArrayList<>();
    }
    return availableItems.stream()
        .map(ProcurementItem::getMaterialDescription)
        .filter(Objects::nonNull)
        .sorted()
        .collect(Collectors.toList());
  }

  // ── Helpers ───────────────────────────────────────────────────────────────

  private String buildCombinedContent() {
    if (uploadedDocuments != null && !uploadedDocuments.isEmpty()) {
      StringBuilder sb = new StringBuilder();
      for (UploadedDocumentEntry doc : uploadedDocuments) {
        if (!doc.isPdf()) {
          sb.append("--- DOCUMENT: ").append(doc.getFileName()).append(" ---\n");
          sb.append(doc.getContent()).append("\n\n");
        }
      }
      String combined = sb.toString().trim();
      return combined.isEmpty() ? null : combined;
    }
    return null;
  }

  private void resetAssistantState() {
    assistantUploadedFileName = null;
    assistantAwaitingConfirmation = Boolean.FALSE;
    assistantParseFeedback = null;
    uploadedDocuments = new ArrayList<>();
    agentUserMessage = null;
    agentChatHistory = new ArrayList<>();
  }

  private String getFeedbackOrDefault(Map<String, Object> result, String defaultFeedback) {
    if (result == null) {
      return defaultFeedback;
    }
    Object feedback = result.get("parseFeedback");
    return (feedback != null && !feedback.toString().isBlank()) ? feedback.toString() : defaultFeedback;
  }

  // ── Getters and Setters ───────────────────────────────────────────────────

  public ProcurementRequest getRequest() { return request; }
  public void setRequest(ProcurementRequest request) { this.request = request; }

  public List<String> getUnitOptions() { return UNIT_OPTIONS; }

  public RequestPriority[] getPriorityValues() { return RequestPriority.values(); }

  public boolean isAssistantUploadEnabled() { return request != null; }

  public String getAssistantUploadedFileName() { return assistantUploadedFileName; }
  public void setAssistantUploadedFileName(String assistantUploadedFileName) {
    this.assistantUploadedFileName = assistantUploadedFileName;
  }

  public Boolean getAssistantAwaitingConfirmation() { return assistantAwaitingConfirmation; }
  public void setAssistantAwaitingConfirmation(Boolean assistantAwaitingConfirmation) {
    this.assistantAwaitingConfirmation = assistantAwaitingConfirmation;
  }

  public String getAssistantParseFeedback() { return assistantParseFeedback; }
  public void setAssistantParseFeedback(String assistantParseFeedback) {
    this.assistantParseFeedback = assistantParseFeedback;
  }

  public List<UploadedDocumentEntry> getUploadedDocuments() { return uploadedDocuments; }
  public void setUploadedDocuments(List<UploadedDocumentEntry> uploadedDocuments) {
    this.uploadedDocuments = uploadedDocuments;
  }

  public String getAgentUserMessage() { return agentUserMessage; }
  public void setAgentUserMessage(String agentUserMessage) {
    this.agentUserMessage = agentUserMessage;
  }

  public List<AssistantChatMessage> getAgentChatHistory() { return agentChatHistory; }
  public void setAgentChatHistory(List<AssistantChatMessage> agentChatHistory) {
    this.agentChatHistory = agentChatHistory;
  }

  public List<ProcurementItem> getAvailableItems() {
    return availableItems;
  }
}
