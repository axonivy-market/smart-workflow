package com.axonivy.utils.smart.workflow.demo.assistant;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.file.UploadedFile;

import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.process.call.SubProcessCallStartEvent;
import ch.ivyteam.ivy.process.call.SubProcessSearchFilter;
import ch.ivyteam.ivy.process.call.SubProcessSearchFilter.SearchScope;
import ch.ivyteam.ivy.security.exec.Sudo;
import jakarta.faces.event.ActionEvent;

public interface AssistantUploadSupport<T> {

  String ATTR_FILE_NAME = "fileName";

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

  interface Cms {
    String BASE              = "/Dialogs/com/axonivy/utils/smart/workflow/demo/erp/supplier/onboarding/components/SupplierAiAssistant/";
    String NO_FILE_UPLOADED  = BASE + "NoFileUploadedMessage";
    String INVALID_FILE      = BASE + "InvalidFileMessage";
    String FILE_EMPTY        = BASE + "FileEmptyMessage";
    String FILES_QUEUED_TPL  = BASE + "FilesQueuedTemplate";
    String FILES_READY_TPL   = BASE + "FilesReadyTemplate";
    String FILE_READY        = BASE + "FileUploadedReadyMessage";
    String CONFIRM_PARSE     = BASE + "ConfirmParseRequired";
    String NO_VALUES         = BASE + "NoValuesExtracted";
    String PARSE_SUCCESS     = BASE + "ParseSuccess";
    String PARSE_FAILED_TPL  = BASE + "ParseFailedTemplate";
  }

  interface Format {
    String DOCUMENT_SEPARATOR = "--- DOCUMENT: %s ---\n";
    String GUIDANCE_HEADER    = "Question Handling Guidelines:\n";
    String GUIDANCE_LINE      = "- When the user asks \"%s\": %s\n";
    String DEFAULT_AGENT_ERROR = "I could not process your question.";
    String AGENT_ERROR        = "Error: %s";
  }

  interface Param {
    String QUESTION         = "question";
    String CHAT_HISTORY     = "chatHistory";
    String GUIDANCE_CONTEXT = "guidanceContext";
  }

  default void removeUploadedDocument(ActionEvent event) {
    removeUploadedDocument((String) event.getComponent().getAttributes().get(ATTR_FILE_NAME));
  }

  default void removeUploadedDocument(String fileName) {
    List<UploadedDocumentEntry> updated = new ArrayList<>(getUploadedDocuments());
    updated.removeIf(d -> d.getFileName().equals(fileName));
    setUploadedDocuments(updated);
    if (updated.isEmpty()) {
      setAssistantAwaitingConfirmation(Boolean.FALSE);
      setAssistantParseFeedback(null);
      setAssistantUploadedFileName(null);
    } else {
      setAssistantParseFeedback(Ivy.cms().co(Cms.FILES_READY_TPL, java.util.Arrays.asList(updated.size())));
      setAssistantUploadedFileName(Ivy.cms().co(Cms.FILES_QUEUED_TPL, java.util.Arrays.asList(updated.size())));
    }
  }

  default void addUploadedDocument(FileUploadEvent event) {
    UploadedFile uploadedFile = event != null ? event.getFile() : null;
    String fileName = uploadedFile != null ? uploadedFile.getFileName() : null;
    byte[] content  = uploadedFile != null ? uploadedFile.getContent()  : null;
    addUploadedDocument(fileName, content);
  }

  default void addUploadedDocument(String fileName, byte[] content) {
    if (fileName == null || fileName.trim().isEmpty()) {
      setAssistantParseFeedback(Ivy.cms().co(Cms.NO_FILE_UPLOADED));
      return;
    }

    String lowerFileName = fileName.toLowerCase(Locale.ROOT);
    if (!(lowerFileName.endsWith(".txt") || lowerFileName.endsWith(".md") || lowerFileName.endsWith(".pdf"))) {
      setAssistantParseFeedback(Ivy.cms().co(Cms.INVALID_FILE));
      return;
    }

    if (content == null || content.length == 0) {
      setAssistantParseFeedback(Ivy.cms().co(Cms.FILE_EMPTY));
      return;
    }

    List<UploadedDocumentEntry> updated = new ArrayList<>(getUploadedDocuments());
    updated.add(UploadedDocumentEntryFactory.of(fileName.trim(), content));
    setUploadedDocuments(updated);
    setAssistantUploadedFileName(Ivy.cms().co(Cms.FILES_QUEUED_TPL, java.util.Arrays.asList(updated.size())));
    setAssistantAwaitingConfirmation(Boolean.TRUE);
    setAssistantParseFeedback(Ivy.cms().co(Cms.FILES_READY_TPL, java.util.Arrays.asList(updated.size())));
  }

  default void uploadAssistantDocument(FileUploadEvent event) {
    resetAssistantState();

    UploadedFile uploadedFile = event != null ? event.getFile() : null;
    if (uploadedFile == null) {
      setAssistantParseFeedback(Ivy.cms().co(Cms.NO_FILE_UPLOADED));
      return;
    }

    String fileName = uploadedFile.getFileName();
    if (fileName == null || fileName.trim().isEmpty()) {
      setAssistantParseFeedback(Ivy.cms().co(Cms.NO_FILE_UPLOADED));
      return;
    }

    String lowerFileName = fileName.toLowerCase(Locale.ROOT);
    if (!(lowerFileName.endsWith(".txt") || lowerFileName.endsWith(".md") || lowerFileName.endsWith(".pdf"))) {
      setAssistantParseFeedback(Ivy.cms().co(Cms.INVALID_FILE));
      return;
    }

    byte[] content = uploadedFile.getContent();
    if (content == null || content.length == 0) {
      setAssistantParseFeedback(Ivy.cms().co(Cms.FILE_EMPTY));
      return;
    }

    setAssistantUploadedFileName(fileName);
    setAssistantUploadedContent(new String(content, StandardCharsets.UTF_8));
    setAssistantAwaitingConfirmation(Boolean.TRUE);
    setAssistantParseFeedback(Ivy.cms().co(Cms.FILE_READY));
  }

  @SuppressWarnings("unchecked")
  default Object confirmAssistantDocumentParse() {
    List<UploadedDocumentEntry> docs = getUploadedDocuments();
    UploadedDocumentEntry pdfDoc = (docs != null)
        ? docs.stream().filter(UploadedDocumentEntryFactory::isPdf).findFirst().orElse(null)
        : null;

    Map<String, Object> params = new HashMap<>();
    params.put("caseUuid", Ivy.wfCase().uuid());
    if (pdfDoc != null) {
      params.put("inputStream", UploadedDocumentEntryFactory.getInputStream(pdfDoc));
    } else {
      String contentToparse = buildCombinedContent();
      if (StringUtils.isBlank(contentToparse)) {
        setAssistantAwaitingConfirmation(Boolean.FALSE);
        setAssistantParseFeedback(Ivy.cms().co(Cms.CONFIRM_PARSE));
        return null;
      }
      params.put("content", contentToparse);
    }

    try {

      Map<String, Object> result = startSubProcessInSecurityContext(
          getParseSubProcessSignature(), params);
      T parsedDraft = result != null ? (T) result.get(getParsedResultKey()) : null;
      setAssistantParsedDraft(parsedDraft);

      if (parsedDraft == null) {
        setAssistantAwaitingConfirmation(Boolean.TRUE);
        setAssistantParseFeedback(Ivy.cms().co(Cms.NO_VALUES));
        return null;
      }

      applyParsedDraft(parsedDraft);
      setAssistantAwaitingConfirmation(Boolean.FALSE);
      setAssistantParseFeedback(Ivy.cms().co(Cms.PARSE_SUCCESS));
    } catch (Exception ex) {
      setAssistantAwaitingConfirmation(Boolean.TRUE);
      setAssistantParseFeedback(Ivy.cms().co(Cms.PARSE_FAILED_TPL, java.util.Arrays.asList(ex.getMessage())));
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
          sb.append(String.format(Format.DOCUMENT_SEPARATOR, doc.getFileName()));
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
    StringBuilder sb = new StringBuilder(Format.GUIDANCE_HEADER);
    for (AgentGuidance g : guidance) {
      sb.append(String.format(Format.GUIDANCE_LINE, g.getQuestionPattern(), g.getInstruction()));
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
      params.put(Param.QUESTION, latestQuestion);
      params.put(Param.CHAT_HISTORY, formattedHistory);
      params.put(Param.GUIDANCE_CONTEXT, compileGuidanceContext());

      Map<String, Object> result =
          startSubProcessInSecurityContext(signature, params);

      String response = (result != null && result.get(getAgentResponseKey()) != null)
          ? result.get(getAgentResponseKey()).toString()
          : Format.DEFAULT_AGENT_ERROR;

      List<AssistantChatMessage> updatedHistory = new ArrayList<>(getAgentChatHistory());
      updatedHistory.add(AssistantChatMessageFactory.of("assistant", response));
      setAgentChatHistory(updatedHistory);
    } catch (Exception e) {
      List<AssistantChatMessage> updatedHistory = new ArrayList<>(getAgentChatHistory());
      updatedHistory.add(AssistantChatMessageFactory.of("assistant", String.format(Format.AGENT_ERROR, e.getMessage())));
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

  static Map<String, Object> startSubProcessInSecurityContext(String signature, Map<String, Object> params) {
    return Sudo.get(() -> {
      var filter = SubProcessSearchFilter.create()
          .setSearchScope(SearchScope.SECURITY_CONTEXT)
          .setSignature(signature).toFilter();

      var subProcessStartList = SubProcessCallStartEvent.find(filter);
      if (subProcessStartList.isEmpty()) {
        return Map.of();
      }
      var subProcessStart = subProcessStartList.get(0);

      Map<String, Object> resolvedParams = Optional.ofNullable(params).orElse(Map.of());
      if (resolvedParams.isEmpty()) {
        return subProcessStart.call().asMap();
      }

      Map<String, Object> result = new HashMap<>();
      List<Entry<String, Object>> entryList = new ArrayList<>(resolvedParams.entrySet());
      for (Entry<String, Object> entry : entryList) {
        if (entryList.indexOf(entry) != entryList.size() - 1) {
          subProcessStart.withParam(entry.getKey(), entry.getValue());
        } else {
          result = subProcessStart.withParam(entry.getKey(), entry.getValue()).call().asMap();
        }
      }
      return result;
    });
  }
}
