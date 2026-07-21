package com.axonivy.utils.smart.workflow.governance.history.analytic.report.analyze.service;

import java.util.Locale;
import java.util.Optional;

import com.axonivy.utils.smart.workflow.governance.history.entity.AgentConversationEntry;
import com.axonivy.utils.smart.workflow.utils.JsonUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

import ch.ivyteam.ivy.environment.Ivy;

class ConversationParserService {

  private interface Field {
    String MESSAGES = "messages";
    String TEXT     = "text";
    String TYPE     = "type";
    String CONTENTS = "contents";
  }

  private interface MessageType {
    String TEXT    = "TEXT";
    String UNKNOWN = "UNKNOWN";
  }

  private static final String WARN_PARSE_FAILURE = "ConversationParserService: failed to parse messagesJson: ";

  private ConversationParserService() {}

  public static Optional<String> parse(AgentConversationEntry entry) {
    try {
      var node = JsonUtils.getObjectMapper().readTree(entry.getMessagesJson());
      return Optional.of(buildText(node.isArray() ? node : node.path(Field.MESSAGES)));
    } catch (JsonProcessingException e) {
      Ivy.log().warn(WARN_PARSE_FAILURE + e.getMessage());
      return Optional.empty();
    }
  }

  private static String buildText(JsonNode messages) {
    if (!messages.isArray()) {
      return "";
    }
    StringBuilder text = new StringBuilder();
    for (var message : messages) {
      String type = message.path(Field.TYPE).asText(MessageType.UNKNOWN).toUpperCase(Locale.ROOT);
      String messageText = extractText(message);
      if (messageText != null && !messageText.isBlank()) {
        text.append("[").append(type).append("] ").append(messageText).append("\n");
      }
    }
    return text.toString();
  }

  private static String extractText(JsonNode message) {
    var textNode = message.path(Field.TEXT);
    if (!textNode.isMissingNode()) {
      return textNode.asText();
    }
    var contents = message.path(Field.CONTENTS);
    if (contents.isArray()) {
      StringBuilder text = new StringBuilder();
      for (var content : contents) {
        var contentText = content.path(Field.TEXT);
        if (!contentText.isMissingNode() && MessageType.TEXT.equalsIgnoreCase(content.path(Field.TYPE).asText(""))) {
          text.append(contentText.asText());
        }
      }
      return !text.isEmpty() ? text.toString() : null;
    }
    return null;
  }
}
