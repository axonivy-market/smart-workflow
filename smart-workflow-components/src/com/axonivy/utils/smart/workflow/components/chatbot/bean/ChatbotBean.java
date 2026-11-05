package com.axonivy.utils.smart.workflow.components.chatbot.bean;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.primefaces.PrimeFaces;

import com.axonivy.utils.smart.workflow.components.chatbot.message.entity.SmartWorkflowErrorMessage;
import com.axonivy.utils.smart.workflow.components.chatbot.message.entity.SmartWorkflowMessage;
import com.axonivy.utils.smart.workflow.components.chatbot.message.enums.AnswerBlockReason;
import com.axonivy.utils.smart.workflow.components.chatbot.service.IvyAdapterService;
import com.axonivy.utils.smart.workflow.components.chatbot.utils.ChatbotUtils;

import ch.ivyteam.ivy.environment.Ivy;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;

@Named("chatbotBean")
@ViewScoped
public class ChatbotBean implements Serializable {
  private static final long serialVersionUID = 1L;

  private static final String CHAT_SIGNATURE = "askChatbotAgent(String)";

  static final int MAX_MESSAGE_LENGTH = 2000;

  interface Param {
    String QUERY = "query";
    String ACCEPTED = "accepted"; // read by AiChatbot.afterSend in chatbot.js
  }

  interface Variable {
    String ANSWER = "answer";
    String BLOCK_REASON = "blockReason";
  }

  interface Message {
    String ANSWER_FAILED = "Chatbot failed to answer the question.";
    String EMPTY_ANSWER = "Chatbot received an empty answer from the AI.";
  }

  interface Pattern {
    String ANSWER_BLOCKED = "Chatbot answer was blocked: %s";
    String CMS = "/Dialogs/com/axonivy/utils/ai/components/Chatbot/%s";
  }

  private List<SmartWorkflowMessage> chatHistory = new ArrayList<>();
  private String userMessage;
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
    String question = userMessage == null ? "" : userMessage.trim();
    if (question.isEmpty()) {
      reportAccepted(false);
      return;
    }
    error = null;
    chatHistory.add(SmartWorkflowMessage.user(question));
    userMessage = "";
    reportAccepted(true);
  }

  protected void reportAccepted(boolean accepted) {
    PrimeFaces.current().ajax().addCallbackParam(Param.ACCEPTED, accepted);
  }

  public void retry() {
    if (error != null && error.isRetryAllowed()) {
      error = null;
    }
  }

  public void newConversation() {
    chatHistory = new ArrayList<>();
    userMessage = "";
    error = null;
  }

  public void answer() {
    if (!isAnswerPending()) {
      return;
    }
    try {
      Map<String, Object> result = IvyAdapterService.startSubProcessInSecurityContext(
          CHAT_SIGNATURE, Param.QUERY, ChatbotUtils.toAgentQuery(chatHistory));
      handleResults(result);
    } catch (Exception e) {
      Ivy.log().error(Message.ANSWER_FAILED, e);
      error = new SmartWorkflowErrorMessage(cmsText("ErrorMessage"));
    }
  }

  // stays true while answer() runs, so this relies on p:remoteCommand queuing its requests;
  // after a failure, retry() clears the error to make the question pending again
  private boolean isAnswerPending() {
    return error == null && !chatHistory.isEmpty() && chatHistory.getLast().isUser();
  }

  private void handleResults(Map<String, Object> result) {
    if (result.get(Variable.BLOCK_REASON) instanceof AnswerBlockReason reason) {
      Ivy.log().warn(String.format(Pattern.ANSWER_BLOCKED, reason));
      boolean questionRejected = reason == AnswerBlockReason.INPUT_GUARDRAIL;
      if (questionRejected) {
        userMessage = chatHistory.removeLast().getContent();
      }
      error = new SmartWorkflowErrorMessage(cmsText(reason.getCmsKey()), !questionRejected);
      return;
    }
    String answer = (String) result.get(Variable.ANSWER);
    if (answer == null || answer.isBlank()) {
      Ivy.log().warn(Message.EMPTY_ANSWER);
      error = new SmartWorkflowErrorMessage(cmsText("EmptyAnswerMessage"));
      return;
    }
    chatHistory.add(SmartWorkflowMessage.ai(answer));
  }

  private static String cmsText(String key) {
    return Ivy.cms().co(String.format(Pattern.CMS, key));
  }
}
