package com.axonivy.utils.smart.workflow.governance.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.axonivy.utils.smart.workflow.governance.history.entity.AgentConversationEntry;

import org.apache.commons.lang3.StringUtils;

import ch.ivyteam.ivy.environment.Ivy;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;

public class MessageViewModelParser {

  public List<MessageViewModel> parse(AgentConversationEntry entry) {
    return Optional.ofNullable(entry)
        .map(AgentConversationEntry::getMessagesJson)
        .map(json -> {
          try {
            List<ChatMessage> messages = ChatMessageDeserializer.messagesFromJson(json);
            List<MessageViewModel> result = new ArrayList<>();
            for (ChatMessage msg : messages) {
              switch (msg) {
                case SystemMessage sm ->
                    result.add(new MessageViewModel(Role.SYSTEM, sm.text()));
                case UserMessage um -> {
                  String text = um.contents().stream()
                      .filter(content -> content instanceof TextContent)
                      .map(content -> ((TextContent) content).text())
                      .collect(Collectors.joining("\n"));
                  result.add(new MessageViewModel(Role.USER, text));
                }
                case AiMessage am ->
                    result.add(new MessageViewModel(Role.ASSISTANT, am.text()));
                case ToolExecutionResultMessage tm ->
                    result.add(new MessageViewModel(Role.TOOL, "Tool: " + tm.toolName() + "\n" + tm.text()));
                default -> { /* unsupported message type — skip */ }
              }
            }
            return result;
          } catch (IllegalArgumentException e) {
            Ivy.log().warn("MessageViewModelParser: failed for entry {0}: {1}", entry.getTaskUuid(), e.getMessage());
            return List.<MessageViewModel>of();
          }
        })
        .orElse(List.of());
  }

  public List<MessageViewModel> getSystemMessages(AgentConversationEntry entry) {
    return parse(entry).stream()
        .filter(msg -> Role.SYSTEM.equals(msg.getRole()))
        .collect(Collectors.toList());
  }

  public List<MessageViewModel> getUserMessages(AgentConversationEntry entry) {
    return parse(entry).stream()
        .filter(msg -> Role.USER.equals(msg.getRole()))
        .collect(Collectors.toList());
  }

  public List<MessageViewModel> getAssistantMessages(AgentConversationEntry entry) {
    return parse(entry).stream()
        .filter(msg -> Role.ASSISTANT.equals(msg.getRole()))
        .collect(Collectors.toList());
  }

  public interface Role {
    String SYSTEM    = "system";
    String USER      = "user";
    String ASSISTANT = "assistant";
    String TOOL      = "tool";
  }

  public static class MessageViewModel {
    private final String role;
    private final String text;

    public MessageViewModel(String role, String text) {
      this.role = role;
      this.text = StringUtils.defaultString(text);
    }

    public String getRole() {
      return role;
    }

    public String getText() {
      return text;
    }
  }
}
