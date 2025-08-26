package com.axonivy.utils.ai.axon.ivy.ai.demo.bean;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import com.axonivy.utils.ai.axon.ivy.ai.demo.service.IvyAdapterService;

import ch.ivyteam.ivy.environment.Ivy;

@ManagedBean
@ViewScoped
public class ChatBean {

  public static class ChatMessage {
    private String role;
    private String content;
    private String timestamp;

    public ChatMessage(String role, String content) {
      this.role = role;
      this.content = content;
      this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm  dd-MM-YYYY"));
    }

    public String getRole() {
      return role;
    }

    public String getContent() {
      return content;
    }

    public String getTimestamp() {
      return timestamp;
    }

    public String getHtmlContent() {
      if ("assistant".equals(role) || "bot".equals(role)) {
        
      }
      return content;
    }
  }

  private String userMessage;
  private List<ChatMessage> chatHistory = new ArrayList<>();
  private boolean botTyping = false;

  public boolean isBotTyping() {
    return botTyping;
  }

  public void setBotTyping(boolean botTyping) {
    this.botTyping = botTyping;
  }

  public String getUserMessage() {
    return userMessage;
  }

  public void setUserMessage(String userMessage) {
    this.userMessage = userMessage;
  }

  public List<ChatMessage> getChatHistory() {
    return chatHistory;
  }

  public void sendMessage() {
    if (userMessage == null || userMessage.trim().isEmpty()) {
      return;
    }

    chatHistory.add(new ChatMessage("user", userMessage));
    botTyping = true;

    try {
      Map<String, Object> params = new HashMap<>();
      Ivy.log().error("send messgae is: " + getAllUserMessage());
      params.put("question", getAllUserMessage());
      params.put("username", Ivy.session().getSessionUserName());
      Map<String, Object> result = IvyAdapterService.startSubProcessInSecurityContext("askAI(String,String)", params);
      String response = result.get("aiResponse") != null ? result.get("aiResponse").toString() : "I don't get your question";
      chatHistory.add(new ChatMessage("bot", response));
    } catch (Exception e) {
      chatHistory.add(new ChatMessage("bot", "Error: " + e.getMessage()));
    }
    botTyping = false;
    userMessage = "";
  }
  
  private String getAllUserMessage() {
    StringBuilder builder = new StringBuilder();
    for (ChatMessage msg:chatHistory) {
      if (msg.getRole().equals("user")) {
        builder.append(msg.getContent()+". ");
      }
    }
    return builder.toString();
  }
}
