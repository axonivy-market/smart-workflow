package com.axonivy.utils.smart.workflow.demo.bean;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import com.axonivy.utils.smart.workflow.demo.service.IvyAdapterService;

import ch.ivyteam.ivy.environment.Ivy;

@ManagedBean
@ViewScoped
public class AxonIvyInfoChatBean {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm  dd-MM-yyyy");

    public static class ChatMessage {
        private String role;
        private String content;
        private String timestamp;

        public ChatMessage(String role, String content) {
            this.role = role;
            this.content = content;
            this.timestamp = LocalDateTime.now().format(TIME_FORMAT);
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
            return content;
        }
    }

    private String userMessage;
    private List<ChatMessage> chatHistory = new ArrayList<>();
    private boolean loading;

    public AxonIvyInfoChatBean() {
        chatHistory.add(new ChatMessage("bot",
                "Hello! I can help you find information about Axon Ivy. What would you like to know?"));
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

    public boolean isLoading() {
        return loading;
    }

    public void sendMessage() {
        if (userMessage == null || userMessage.trim().isEmpty()) {
            return;
        }
        chatHistory.add(new ChatMessage("user", userMessage));
        loading = true;
        userMessage = "";
    }

    public void getAnswer() {
        try {
            String lastUserMessage = getLastUserMessage();
            if (lastUserMessage.isEmpty()) {
                return;
            }
            Map<String, Object> params = new HashMap<>();
            params.put("question", lastUserMessage);
            Map<String, Object> result = IvyAdapterService
                    .startSubProcessInSecurityContext("askAxonIvyInfo(String)", params);
            String response = result.get("aiResponse") != null
                    ? result.get("aiResponse").toString()
                    : "I couldn't find information about that. Could you try rephrasing your question?";
            chatHistory.add(new ChatMessage("bot", response));
        } catch (Exception e) {
            Ivy.log().error("Error getting AI response", e);
            chatHistory.add(new ChatMessage("bot",
                    "Sorry, something went wrong while searching. Please try again."));
        } finally {
            loading = false;
        }
    }

    private String getLastUserMessage() {
        for (int i = chatHistory.size() - 1; i >= 0; i--) {
            ChatMessage msg = chatHistory.get(i);
            if ("user".equals(msg.getRole())) {
                return msg.getContent();
            }
        }
        return "";
    }
}
