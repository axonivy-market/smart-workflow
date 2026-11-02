package com.axonivy.utils.smart.workflow.components.chatbot.bean;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.axonivy.utils.smart.workflow.components.chatbot.message.entity.SmartWorkflowErrorMessage;
import com.axonivy.utils.smart.workflow.components.chatbot.message.entity.SmartWorkflowMessage;
import com.axonivy.utils.smart.workflow.components.chatbot.message.enums.AnswerBlockReason;
import com.axonivy.utils.smart.workflow.components.chatbot.utils.ChatbotUtils;
import com.axonivy.utils.smart.workflow.components.chatbot.service.IvyAdapterService;

import org.primefaces.PrimeFaces;

import ch.ivyteam.ivy.environment.Ivy;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;

@Named("chatbotBean")
@ViewScoped
public class ChatbotBean implements Serializable {
  private static final long serialVersionUID = 1L;

  private static final String CHAT_SIGNATURE = "chat(String,List<com.axonivy.utils.smart.workflow.components.chatbot.message.entity.SmartWorkflowMessage>)";

  private static final String CMS_PATH = "/Dialogs/com/axonivy/utils/ai/components/Chatbot/";

  /** Also the input's maxlength, so the browser and the server apply the same limit. */
  static final int MAX_MESSAGE_LENGTH = 2000;

  // ajax callback param read by AiChatbot.afterSend: false keeps the text in the input and asks no answer
  private static final String ACCEPTED_PARAM = "accepted";

  private List<SmartWorkflowMessage> chatHistory = new ArrayList<>();
  private String userMessage;
  private String pendingQuestion;
  // shown below the last question until the user retries or asks something else; never part of the history
  private SmartWorkflowErrorMessage error;

  public List<SmartWorkflowMessage> getChatHistory() {
    return chatHistory;
  }

  public String getUserMessage() {
    return userMessage;
  }

  public void setUserMessage(String userMessage) {
    this.userMessage = userMessage;
  }

  public SmartWorkflowErrorMessage getError() {
    return error;
  }

  public int getMaxMessageLength() {
    return MAX_MESSAGE_LENGTH;
  }

  public void sendMessage() {
    // the browser counts a line break as one character for maxlength but submits it as \r\n
    String question = userMessage == null ? "" : userMessage.replace("\r\n", "\n").trim();
    if (question.isEmpty()) {
      reportAccepted(false);
      return;
    }
    if (question.length() > MAX_MESSAGE_LENGTH) {
      // the user has to shorten the message, so asking again would not help
      error = new SmartWorkflowErrorMessage(
          Ivy.cms().co(CMS_PATH + "MessageTooLong", List.of(MAX_MESSAGE_LENGTH)), null, false);
      reportAccepted(false);
      return;
    }
    error = null;
    pendingQuestion = question;
    chatHistory.add(SmartWorkflowMessage.user(pendingQuestion));
    userMessage = "";
    reportAccepted(true);
  }

  private static void reportAccepted(boolean accepted) {
    PrimeFaces.current().ajax().addCallbackParam(ACCEPTED_PARAM, accepted);
  }

  /** Asks the last unanswered question again after an error. */
  public void retry() {
    if (error == null || !error.isRetryable() || chatHistory.isEmpty()) {
      return;
    }
    SmartWorkflowMessage lastMessage = chatHistory.get(chatHistory.size() - 1);
    if (lastMessage.isUser()) {
      error = null;
      pendingQuestion = lastMessage.getContent();
    }
  }

  public void newConversation() {
    chatHistory = new ArrayList<>();
    userMessage = "";
    pendingQuestion = null;
    error = null;
  }

  public void answer() {
    if (pendingQuestion == null) {
      return;
    }
    String question = pendingQuestion;
    pendingQuestion = null;
    // the agent process appends the question itself, so pass the history without it.
    // Callable processes only accept Ivy's own List type for List parameters.
    List<SmartWorkflowMessage> previousHistory = ch.ivyteam.ivy.scripting.objects.List.create(
        SmartWorkflowMessage.class, new ArrayList<>(chatHistory.subList(0, chatHistory.size() - 1)));
    try {
      Map<String, Object> result = IvyAdapterService.startSubProcessInSecurityContext(
          CHAT_SIGNATURE,
          Map.of(
              "userMessage", question,
              "history", previousHistory));
      handleResults(result);
    } catch (Exception e) {
      Ivy.log().error("Chatbot failed to answer the question.", e);
      error = new SmartWorkflowErrorMessage(Ivy.cms().co(CMS_PATH + "ErrorMessage"), ChatbotUtils.errorDetails(e));
    }
  }

  @SuppressWarnings("unchecked")
  private void handleResults(Map<String, Object> result) {
    // a guardrail or the circuit breaker refused: show our own text, the raw error only in the details
    if (result.get("blockReason") instanceof AnswerBlockReason reason) {
      error = new SmartWorkflowErrorMessage(
          Ivy.cms().co(CMS_PATH + reason.getCmsKey()), (String) result.get("errorDetails"));
      return;
    }
    List<SmartWorkflowMessage> processHistory = (List<SmartWorkflowMessage>) result.get("history");
    // the process only appends an answer when the AI returned text; otherwise keep the question for a retry
    if (processHistory == null || processHistory.isEmpty() || processHistory.get(processHistory.size() - 1).isUser()) {
      Ivy.log().warn("Chatbot received an empty answer from the AI.");
      error = new SmartWorkflowErrorMessage(Ivy.cms().co(CMS_PATH + "EmptyAnswerMessage"), null);
      return;
    }
    chatHistory = new ArrayList<>(processHistory);
  }
}
