package com.axonivy.utils.smart.workflow.governance.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.smart.workflow.governance.history.entity.AgentConversationEntry;

import ch.ivyteam.ivy.environment.Ivy;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;

public class MessageViewModelParser {

  private final Map<CacheKey, Map<Role, List<MessageViewModel>>> cache = new HashMap<>();

  public Map<Role, List<MessageViewModel>> parseByRole(AgentConversationEntry entry) {
    if (entry == null || entry.getMessagesJson() == null) {
      return Map.of();
    }
    CacheKey cacheKey = new CacheKey(entry.getCaseUuid(), entry.getTaskUuid(), entry.getAgentId());
    return cache.computeIfAbsent(cacheKey, key -> groupByRole(parseJson(entry.getMessagesJson(), entry.getTaskUuid())));
  }

  private List<MessageViewModel> parseJson(String json, String taskUuid) {
    try {
      List<ChatMessage> messages = ChatMessageDeserializer.messagesFromJson(json);
      List<MessageViewModel> result = new ArrayList<>();
      for (ChatMessage msg : messages) {
        switch (msg) {
          case SystemMessage sm ->
              result.add(new MessageViewModel(Role.SYSTEM, sm.text()));
          case UserMessage um -> {
            String text = um.contents().stream()
                .filter(TextContent.class::isInstance)
                .map(TextContent.class::cast)
                .map(TextContent::text)
                .collect(Collectors.joining("\n"));
            result.add(new MessageViewModel(Role.USER, text));
          }
          case AiMessage am ->
              result.add(new MessageViewModel(Role.ASSISTANT, am.text()));
          case ToolExecutionResultMessage tm ->
              result.add(new MessageViewModel(Role.TOOL, formatToolMessage(tm)));
          default -> { }
        }
      }
      return result;
    } catch (IllegalArgumentException e) {
      Ivy.log().warn("MessageViewModelParser: failed for entry {0}", e, taskUuid);
      return List.of();
    }
  }

  private static String formatToolMessage(ToolExecutionResultMessage tm) {
    return "Tool: " + tm.toolName() + "\n" + tm.text();
  }

  private static Map<Role, List<MessageViewModel>> groupByRole(List<MessageViewModel> messages) {
    return Map.copyOf(messages.stream().collect(Collectors.groupingBy(MessageViewModel::getRole)));
  }

  private record CacheKey(String caseUuid, String taskUuid, String agentId) {}

  public enum Role {
    SYSTEM, USER, ASSISTANT, TOOL
  }

  public record MessageViewModel(Role role, String text) {
    public MessageViewModel {
      text = StringUtils.defaultString(text);
    }

    public Role getRole() {
      return role;
    }

    public String getText() {
      return text;
    }
  }
}
