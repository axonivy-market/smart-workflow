package com.axonivy.utils.smart.workflow.demo.assistant;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.file.UploadedFile;

import com.axonivy.utils.smart.workflow.demo.utils.IvyAdapterService;

import ch.ivyteam.ivy.environment.Ivy;

public interface AssistantUploadSupport<T> {

  T getFormData();

  String getParseSubProcessSignature();

  String getParsedResultKey();

  void applyParsedDraft(T parsedDraft);

  void setAssistantParsedDraft(T draft);

  String getAssistantUploadedContent();

  void setAssistantUploadedFileName(String fileName);

  void setAssistantUploadedContent(String content);

  void setAssistantAwaitingConfirmation(Boolean awaiting);

  void setAssistantParseFeedback(String feedback);

  default List<UploadedDocumentEntry> getUploadedDocuments() {
    return List.of();
  }

  default void setUploadedDocuments(List<UploadedDocumentEntry> docs) {
  }

  default String getAgentSubProcessSignature() {
    return null;
  }

  default String getAgentResponseKey() {
    return "aiResponse";
  }

  default List<AgentGuidance> getAgentGuidance() {
    return List.of();
  }

  default String getAgentUserMessage() {
    return null;
  }

  default void setAgentUserMessage(String message) {
  }

  default List<AssistantChatMessage> getAgentChatHistory() {
    return List.of();
  }

  default void setAgentChatHistory(List<AssistantChatMessage> history) {
  }

  String CMS_SAA              = "/Dialogs/com/axonivy/utils/smart/workflow/demo/erp/supplier/onboarding/components/SupplierAiAssistant/";
  String CMS_NO_FILE_UPLOADED = CMS_SAA + "NoFileUploadedMessage";
  String CMS_INVALID_FILE     = CMS_SAA + "InvalidFileMessage";
  String CMS_FILE_EMPTY       = CMS_SAA + "FileEmptyMessage";
  String CMS_FILES_QUEUED_TPL = CMS_SAA + "FilesQueuedTemplate";
  String CMS_FILES_READY_TPL  = CMS_SAA + "FilesReadyTemplate";
  String CMS_FILE_READY       = CMS_SAA + "FileUploadedReadyMessage";
  String CMS_CONFIRM_PARSE    = CMS_SAA + "ConfirmParseRequired";
  String CMS_NO_VALUES        = CMS_SAA + "NoValuesExtracted";
  String CMS_PARSE_SUCCESS    = CMS_SAA + "ParseSuccess";
  String CMS_PARSE_FAILED_TPL = CMS_SAA + "ParseFailedTemplate";

  String DOCUMENT_SEPARATOR_FORMAT = "--- DOCUMENT: %s ---\n";
  String GUIDANCE_HEADER            = "Question Handling Guidelines:\n";
  String GUIDANCE_LINE_FORMAT       = "- When the user asks \"%s\": %s\n";
  String DEFAULT_AGENT_ERROR_MSG    = "I could not process your question.";
  String AGENT_ERROR_FORMAT         = "Error: %s";

  String PARAM_QUESTION         = "question";
  String PARAM_CHAT_HISTORY     = "chatHistory";
  String PARAM_GUIDANCE_CONTEXT = "guidanceContext";

  default void addUploadedDocument(FileUploadEvent event) {
    UploadedFile uploadedFile = event != null ? event.getFile() : null;
    if (uploadedFile == null) {
      setAssistantParseFeedback(Ivy.cms().co(CMS_NO_FILE_UPLOADED));
      return;
    }

    String fileName = uploadedFile.getFileName();
    if (fileName == null || fileName.trim().isEmpty()) {
      setAssistantParseFeedback(Ivy.cms().co(CMS_NO_FILE_UPLOADED));
      return;
    }

    String lowerFileName = fileName.toLowerCase(Locale.ROOT);
    if (!(lowerFileName.endsWith(".txt") || lowerFileName.endsWith(".md") || lowerFileName.endsWith(".pdf"))) {
      setAssistantParseFeedback(Ivy.cms().co(CMS_INVALID_FILE));
      return;
    }

    byte[] content = uploadedFile.getContent();
    if (content == null || content.length == 0) {
      setAssistantParseFeedback(Ivy.cms().co(CMS_FILE_EMPTY));
      return;
    }

    List<UploadedDocumentEntry> updated = new ArrayList<>(getUploadedDocuments());
    updated.add(UploadedDocumentEntryFactory.of(fileName.trim(), content));
    setUploadedDocuments(updated);
    setAssistantUploadedFileName(Ivy.cms().co(CMS_FILES_QUEUED_TPL, java.util.Arrays.asList(updated.size())));
    setAssistantAwaitingConfirmation(Boolean.TRUE);
    setAssistantParseFeedback(Ivy.cms().co(CMS_FILES_READY_TPL, java.util.Arrays.asList(updated.size())));
  }

  default void uploadAssistantDocument(FileUploadEvent event) {
    resetAssistantState();

    UploadedFile uploadedFile = event != null ? event.getFile() : null;
    if (uploadedFile == null) {
      setAssistantParseFeedback(Ivy.cms().co(CMS_NO_FILE_UPLOADED));
      return;
    }

    String fileName = uploadedFile.getFileName();
    if (fileName == null || fileName.trim().isEmpty()) {
      setAssistantParseFeedback(Ivy.cms().co(CMS_NO_FILE_UPLOADED));
      return;
    }

    String lowerFileName = fileName.toLowerCase(Locale.ROOT);
    if (!(lowerFileName.endsWith(".txt") || lowerFileName.endsWith(".md") || lowerFileName.endsWith(".pdf"))) {
      setAssistantParseFeedback(Ivy.cms().co(CMS_INVALID_FILE));
      return;
    }

    byte[] content = uploadedFile.getContent();
    if (content == null || content.length == 0) {
      setAssistantParseFeedback(Ivy.cms().co(CMS_FILE_EMPTY));
      return;
    }

    setAssistantUploadedFileName(fileName);
    setAssistantUploadedContent(new String(content, StandardCharsets.UTF_8));
    setAssistantAwaitingConfirmation(Boolean.TRUE);
    setAssistantParseFeedback(Ivy.cms().co(CMS_FILE_READY));
  }

  @SuppressWarnings("unchecked")
  default Object confirmAssistantDocumentParse() {
    List<UploadedDocumentEntry> docs = getUploadedDocuments();
    UploadedDocumentEntry pdfDoc = (docs != null)
        ? docs.stream().filter(UploadedDocumentEntryFactory::isPdf).findFirst().orElse(null)
        : null;

    Map<String, Object> params = new HashMap<>();
    if (pdfDoc != null) {
      params.put("inputStream", UploadedDocumentEntryFactory.getInputStream(pdfDoc));
    } else {
      String contentToparse = buildCombinedContent();
      if (StringUtils.isBlank(contentToparse)) {
        setAssistantAwaitingConfirmation(Boolean.FALSE);
        setAssistantParseFeedback(Ivy.cms().co(CMS_CONFIRM_PARSE));
        return null;
      }
      params.put("content", contentToparse);
    }

    try {

      Map<String, Object> result = IvyAdapterService.startSubProcessInSecurityContext(
          getParseSubProcessSignature(), params);
      T parsedDraft = result != null ? (T) result.get(getParsedResultKey()) : null;
      setAssistantParsedDraft(parsedDraft);

      if (parsedDraft == null) {
        setAssistantAwaitingConfirmation(Boolean.TRUE);
        setAssistantParseFeedback(Ivy.cms().co(CMS_NO_VALUES));
        return null;
      }

      applyParsedDraft(parsedDraft);
      setAssistantAwaitingConfirmation(Boolean.FALSE);
      setAssistantParseFeedback(Ivy.cms().co(CMS_PARSE_SUCCESS));
    } catch (Exception ex) {
      setAssistantAwaitingConfirmation(Boolean.TRUE);
      setAssistantParseFeedback(Ivy.cms().co(CMS_PARSE_FAILED_TPL, java.util.Arrays.asList(ex.getMessage())));
      Ivy.log().warn("Assistant parse failed", ex);
    }

    return null;
  }

  default String buildCombinedContent() {
    List<UploadedDocumentEntry> docs = getUploadedDocuments();
    if (docs != null && !docs.isEmpty()) {
      StringBuilder sb = new StringBuilder();
      for (UploadedDocumentEntry doc : docs) {
        if (!UploadedDocumentEntryFactory.isPdf(doc)) {
          sb.append(String.format(DOCUMENT_SEPARATOR_FORMAT, doc.getFileName()));
          sb.append(UploadedDocumentEntryFactory.getContent(doc)).append("\n\n");
        }
      }
      String combined = sb.toString().trim();
      return combined.isEmpty() ? null : combined;
    }
    return getAssistantUploadedContent();
  }

  default void resetAssistantState() {
    setAssistantUploadedFileName(null);
    setAssistantUploadedContent(null);
    setAssistantAwaitingConfirmation(Boolean.FALSE);
    setAssistantParseFeedback(null);
    setAssistantParsedDraft(null);
    setUploadedDocuments(new ArrayList<>());
    setAgentUserMessage(null);
    setAgentChatHistory(new ArrayList<>());
  }

  default String compileGuidanceContext() {
    List<AgentGuidance> guidance = getAgentGuidance();
    if (guidance == null || guidance.isEmpty()) {
      return "";
    }
    StringBuilder sb = new StringBuilder(GUIDANCE_HEADER);
    for (AgentGuidance g : guidance) {
      sb.append(String.format(GUIDANCE_LINE_FORMAT, g.getQuestionPattern(), g.getInstruction()));
    }
    return sb.toString().trim();
  }

  default void sendAgentMessage() {
    String msg = getAgentUserMessage();
    if (StringUtils.isBlank(msg)) {
      return;
    }
    List<AssistantChatMessage> history = new ArrayList<>(getAgentChatHistory());
    history.add(AssistantChatMessageFactory.of("user", msg.trim()));
    setAgentChatHistory(history);
    setAgentUserMessage("");
  }

  default void getAgentAnswer() {
    String signature = getAgentSubProcessSignature();
    if (signature == null) {
      return;
    }

    try {
      List<AssistantChatMessage> history = getAgentChatHistory();
      String latestQuestion = "";
      String formattedHistory = "";

      if (!history.isEmpty()) {
        AssistantChatMessage last = history.get(history.size() - 1);
        latestQuestion = last.getContent();
        List<AssistantChatMessage> previousTurns = history.subList(0, history.size() - 1);
        if (!previousTurns.isEmpty()) {
          int fromIndex = Math.max(0, previousTurns.size() - 10);
          List<AssistantChatMessage> recentTurns = previousTurns.subList(fromIndex, previousTurns.size());
          StringBuilder sb = new StringBuilder();
          for (AssistantChatMessage msg : recentTurns) {
            sb.append(msg.getRole()).append(": ").append(msg.getContent()).append("\n");
          }
          formattedHistory = sb.toString().trim();
        }
      }

      Map<String, Object> params = new HashMap<>();
      params.put(PARAM_QUESTION, latestQuestion);
      params.put(PARAM_CHAT_HISTORY, formattedHistory);
      params.put(PARAM_GUIDANCE_CONTEXT, compileGuidanceContext());

      Map<String, Object> result =
          IvyAdapterService.startSubProcessInSecurityContext(signature, params);

      String response = (result != null && result.get(getAgentResponseKey()) != null)
          ? result.get(getAgentResponseKey()).toString()
          : DEFAULT_AGENT_ERROR_MSG;

      List<AssistantChatMessage> updatedHistory = new ArrayList<>(getAgentChatHistory());
      updatedHistory.add(AssistantChatMessageFactory.of("assistant", response));
      setAgentChatHistory(updatedHistory);
    } catch (Exception e) {
      List<AssistantChatMessage> updatedHistory = new ArrayList<>(getAgentChatHistory());
      updatedHistory.add(AssistantChatMessageFactory.of("assistant", String.format(AGENT_ERROR_FORMAT, e.getMessage())));
      setAgentChatHistory(updatedHistory);
      Ivy.log().warn("Agent chat failed", e);
    }
  }

  default String buildConversationContext() {
    List<AssistantChatMessage> history = getAgentChatHistory();
    if (history == null || history.isEmpty()) {
      return "";
    }
    StringBuilder sb = new StringBuilder();
    for (AssistantChatMessage msg : history) {
      sb.append(msg.getRole()).append(": ").append(msg.getContent()).append("\n");
    }
    return sb.toString().trim();
  }
}
