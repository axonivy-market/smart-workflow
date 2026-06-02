package com.axonivy.utils.smart.workflow.demo.erp.assistant;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
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

/**
 * Generic interface for AI-assisted document upload, structured parsing, and
 * agent-powered chat in HTML dialog forms.
 *
 * <p>Implementations bind a form data type {@code T} to:
 * <ul>
 * <li>A callable subprocess that extracts values from uploaded text/markdown files.</li>
 * <li>An agent subprocess that answers user questions using developer-defined
 * {@link AgentGuidance} records. The guidance records serve two roles:
 * <ol>
 *   <li>Their {@link AgentGuidance#questionPattern()} is displayed as clickable
 *   suggestion chips in the assistant sidebar UI.</li>
 *   <li>Their {@link AgentGuidance#instruction()} is compiled into the agent's
 *   system prompt via {@link #compileGuidanceContext()}.</li>
 * </ol>
 * </li>
 * </ul>
 *
 * <p>Implementors must provide:
 * <ul>
 * <li>{@link #getFormData()} — the target form object to populate</li>
 * <li>{@link #getParseSubProcessSignature()} — the callable subprocess
 * signature for document parsing</li>
 * <li>{@link #getParsedResultKey()} — the result map key for the parsed
 * object</li>
 * <li>{@link #applyParsedDraft(Object)} — field-level mapping from parsed
 * object to form data</li>
 * <li>State accessors: {@link #getAssistantUploadedContent()} and all
 * {@code setAssistant*} setters</li>
 * </ul>
 *
 * <p>To enable agent chat, implementors override:
 * <ul>
 * <li>{@link #getAgentSubProcessSignature()} — the chat agent subprocess</li>
 * <li>{@link #getAgentGuidance()} — question-instruction guidance records</li>
 * <li>Chat state accessors: {@code get/setAgentUserMessage},
 * {@code get/setAgentChatHistory}</li>
 * </ul>
 *
 * @param <T> the type of the parsed result object
 */
public interface AssistantUploadSupport<T> {

  /**
   * Represents a single uploaded document queued for multi-document parsing.
   */
  final class UploadedDocumentEntry {
    private final String fileName;
    private final byte[] data;

    public UploadedDocumentEntry(String fileName, byte[] data) {
      this.fileName = fileName;
      this.data = data;
    }

    public String getFileName() {
      return fileName;
    }

    public String getContent() {
      return new String(data, StandardCharsets.UTF_8);
    }

    public boolean isPdf() {
      return fileName != null && fileName.toLowerCase(Locale.ROOT).endsWith(".pdf");
    }

    public InputStream getInputStream() {
      return new ByteArrayInputStream(data);
    }
  }

  // ── Abstract contract (document upload & parse) ────────────────────────────

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

  // ── Multi-document upload contract (all default for backward compatibility) ─

  /**
   * Returns the list of documents queued for multi-document parsing.
   * Defaults to an empty list (single-document mode).
   */
  default List<UploadedDocumentEntry> getUploadedDocuments() {
    return List.of();
  }

  /**
   * Replaces the queued document list. No-op by default.
   */
  default void setUploadedDocuments(List<UploadedDocumentEntry> docs) {
    // no-op — override in implementor to store state
  }

  // ── Agent contract (all default for backward compatibility) ────────────────

  /**
   * Returns the callable subprocess signature for the chat agent.
   * Example: {@code "askSupplierAssistant(String,String)"}
   *
   * <p>Return {@code null} (the default) to disable agent chat entirely.
   */
  default String getAgentSubProcessSignature() {
    return null;
  }

  /**
   * Returns the result map key for the agent's text response.
   * Defaults to {@code "aiResponse"}.
   */
  default String getAgentResponseKey() {
    return "aiResponse";
  }

  /**
   * Returns the list of guidance records that define how the agent handles
   * different types of questions.
   *
   * <p>Each {@link AgentGuidance} record has two roles:
   * <ol>
   *   <li>{@link AgentGuidance#questionPattern()} — shown as a clickable chip
   *   in the assistant sidebar UI</li>
   *   <li>{@link AgentGuidance#instruction()} — compiled into the agent system
   *   prompt via {@link #compileGuidanceContext()}</li>
   * </ol>
   */
  default List<AgentGuidance> getAgentGuidance() {
    return List.of();
  }

  /**
   * Returns the current chat input text entered by the user.
   * Defaults to {@code null} (no-op implementation).
   */
  default String getAgentUserMessage() {
    return null;
  }

  /**
   * Sets the current chat input text. No-op by default.
   */
  default void setAgentUserMessage(String message) {
    // no-op — override in implementor to store state
  }

  /**
   * Returns the chat conversation history.
   * Defaults to an empty list.
   */
  default List<AssistantChatMessage> getAgentChatHistory() {
    return List.of();
  }

  /**
   * Sets the chat conversation history. No-op by default.
   */
  default void setAgentChatHistory(List<AssistantChatMessage> history) {
    // no-op — override in implementor to store state
  }

  String CMS_SAA = "/Dialogs/com/axonivy/utils/smart/workflow/demo/erp/supplier/onboarding/components/SupplierAiAssistant/";

  // ── Default lifecycle methods (document upload & parse) ───────────────────

  /**
   * Validates and appends the uploaded file to the multi-document queue.
   * Updates feedback to reflect the current number of queued files.
   * Call this instead of {@link #uploadAssistantDocument} to accumulate
   * multiple documents before a single parse invocation.
   */
  default void addUploadedDocument(FileUploadEvent event) {
    UploadedFile uploadedFile = event != null ? event.getFile() : null;
    if (uploadedFile == null) {
      setAssistantParseFeedback(Ivy.cms().co(CMS_SAA + "NoFileUploadedMessage"));
      return;
    }

    String fileName = uploadedFile.getFileName();
    if (fileName == null || fileName.trim().isEmpty()) {
      setAssistantParseFeedback(Ivy.cms().co(CMS_SAA + "NoFileUploadedMessage"));
      return;
    }

    String lowerFileName = fileName.toLowerCase(Locale.ROOT);
    if (!(lowerFileName.endsWith(".txt") || lowerFileName.endsWith(".md") || lowerFileName.endsWith(".pdf"))) {
      setAssistantParseFeedback(Ivy.cms().co("/Dialogs/com/axonivy/utils/smart/workflow/demo/erp/supplier/onboarding/components/SupplierAiAssistant/InvalidFileMessage"));
      return;
    }

    byte[] content = uploadedFile.getContent();
    if (content == null || content.length == 0) {
      setAssistantParseFeedback(Ivy.cms().co(CMS_SAA + "FileEmptyMessage"));
      return;
    }

    List<UploadedDocumentEntry> updated = new ArrayList<>(getUploadedDocuments());
    updated.add(new UploadedDocumentEntry(fileName.trim(), content));
    setUploadedDocuments(updated);
    setAssistantUploadedFileName(Ivy.cms().co(CMS_SAA + "FilesQueuedTemplate", java.util.Arrays.asList(updated.size())));
    setAssistantAwaitingConfirmation(Boolean.TRUE);
    setAssistantParseFeedback(Ivy.cms().co(CMS_SAA + "FilesReadyTemplate", java.util.Arrays.asList(updated.size())));
  }

  default void uploadAssistantDocument(FileUploadEvent event) {
    resetAssistantState();

    UploadedFile uploadedFile = event != null ? event.getFile() : null;
    if (uploadedFile == null) {
      setAssistantParseFeedback(Ivy.cms().co(CMS_SAA + "NoFileUploadedMessage"));
      return;
    }

    String fileName = uploadedFile.getFileName();
    if (fileName == null || fileName.trim().isEmpty()) {
      setAssistantParseFeedback(Ivy.cms().co(CMS_SAA + "NoFileUploadedMessage"));
      return;
    }

    String lowerFileName = fileName.toLowerCase(Locale.ROOT);
    if (!(lowerFileName.endsWith(".txt") || lowerFileName.endsWith(".md") || lowerFileName.endsWith(".pdf"))) {
      setAssistantParseFeedback(Ivy.cms().co("/Dialogs/com/axonivy/utils/smart/workflow/demo/erp/supplier/onboarding/components/SupplierAiAssistant/InvalidFileMessage"));
      return;
    }

    byte[] content = uploadedFile.getContent();
    if (content == null || content.length == 0) {
      setAssistantParseFeedback(Ivy.cms().co(CMS_SAA + "FileEmptyMessage"));
      return;
    }

    setAssistantUploadedFileName(fileName);
    setAssistantUploadedContent(new String(content, StandardCharsets.UTF_8));
    setAssistantAwaitingConfirmation(Boolean.TRUE);
    setAssistantParseFeedback(Ivy.cms().co(CMS_SAA + "FileUploadedReadyMessage"));
  }

  @SuppressWarnings("unchecked")
  default Object confirmAssistantDocumentParse() {
    List<UploadedDocumentEntry> docs = getUploadedDocuments();
    UploadedDocumentEntry pdfDoc = (docs != null)
        ? docs.stream().filter(UploadedDocumentEntry::isPdf).findFirst().orElse(null)
        : null;

    Map<String, Object> params = new HashMap<>();
    if (pdfDoc != null) {
      params.put("inputStream", pdfDoc.getInputStream());
    } else {
      String contentToparse = buildCombinedContent();
      if (StringUtils.isBlank(contentToparse)) {
        setAssistantAwaitingConfirmation(Boolean.FALSE);
        setAssistantParseFeedback(Ivy.cms().co(CMS_SAA + "ConfirmParseRequired"));
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
        setAssistantParseFeedback(
            getFeedbackOrDefault(result, Ivy.cms().co(CMS_SAA + "NoValuesExtracted")));
        return null;
      }

      applyParsedDraft(parsedDraft);
      setAssistantAwaitingConfirmation(Boolean.FALSE);
      setAssistantParseFeedback(
          getFeedbackOrDefault(result, Ivy.cms().co(CMS_SAA + "ParseSuccess")));
    } catch (Exception ex) {
      setAssistantAwaitingConfirmation(Boolean.TRUE);
      setAssistantParseFeedback(Ivy.cms().co(CMS_SAA + "ParseFailedTemplate", java.util.Arrays.asList(ex.getMessage())));
      Ivy.log().warn("Assistant parse failed", ex);
    }

    return null;
  }

  /**
   * Builds the content string to send to the parse subprocess.
   * If multiple documents are queued, concatenates them with named separators.
   * Falls back to {@link #getAssistantUploadedContent()} for single-doc mode.
   */
  default String buildCombinedContent() {
    List<UploadedDocumentEntry> docs = getUploadedDocuments();
    if (docs != null && !docs.isEmpty()) {
      StringBuilder sb = new StringBuilder();
      for (UploadedDocumentEntry doc : docs) {
        if (!doc.isPdf()) {
          sb.append("--- DOCUMENT: ").append(doc.getFileName()).append(" ---\n");
          sb.append(doc.getContent()).append("\n\n");
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

  default String getFeedbackOrDefault(Map<String, Object> result, String defaultFeedback) {
    if (result == null) {
      return defaultFeedback;
    }
    String feedback = (String) result.get("feedback");
    return StringUtils.isNotBlank(feedback) ? feedback : defaultFeedback;
  }

  // ── Default lifecycle methods (agent chat) ────────────────────────────────

  /**
   * Compiles all {@link AgentGuidance} records into a system prompt fragment
   * suitable for injection into the agent subprocess via
   * {@code <%= in.guidanceContext %>} macro expansion.
   *
   * <p>Returns an empty string if no guidance is defined.
   */
  default String compileGuidanceContext() {
    List<AgentGuidance> guidance = getAgentGuidance();
    if (guidance == null || guidance.isEmpty()) {
      return "";
    }
    StringBuilder sb = new StringBuilder("Question Handling Guidelines:\n");
    for (AgentGuidance g : guidance) {
      sb.append(g.toPromptLine()).append("\n");
    }
    return sb.toString().trim();
  }

  /**
   * Adds the current user message to the chat history and clears the input.
   * Called by the UI send action before {@link #getAgentAnswer()}.
   */
  default void sendAgentMessage() {
    String msg = getAgentUserMessage();
    if (StringUtils.isBlank(msg)) {
      return;
    }
    List<AssistantChatMessage> history = new ArrayList<>(getAgentChatHistory());
    history.add(new AssistantChatMessage("user", msg.trim()));
    setAgentChatHistory(history);
    setAgentUserMessage("");
  }

  /**
   * Sends the full conversation context and compiled guidance to the agent
   * subprocess, then appends the response to the chat history.
   *
   * <p>Does nothing if {@link #getAgentSubProcessSignature()} returns
   * {@code null}.
   */
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
      params.put("question", latestQuestion);
      params.put("chatHistory", formattedHistory);
      params.put("guidanceContext", compileGuidanceContext());

      Map<String, Object> result =
          IvyAdapterService.startSubProcessInSecurityContext(signature, params);

      String response = (result != null && result.get(getAgentResponseKey()) != null)
          ? result.get(getAgentResponseKey()).toString()
          : "I could not process your question.";

      List<AssistantChatMessage> updatedHistory = new ArrayList<>(getAgentChatHistory());
      updatedHistory.add(new AssistantChatMessage("assistant", response));
      setAgentChatHistory(updatedHistory);
    } catch (Exception e) {
      List<AssistantChatMessage> updatedHistory = new ArrayList<>(getAgentChatHistory());
      updatedHistory.add(new AssistantChatMessage("assistant", "Error: " + e.getMessage()));
      setAgentChatHistory(updatedHistory);
      Ivy.log().warn("Agent chat failed", e);
    }
  }

  /**
   * Builds the full conversation context string from chat history,
   * including both user and assistant turns for multi-turn context.
   */
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
