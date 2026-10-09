package com.axonivy.utils.smart.workflow.guardrails.adapter;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import dev.langchain4j.data.message.Content;
import static dev.langchain4j.data.message.ContentType.AUDIO;
import static dev.langchain4j.data.message.ContentType.IMAGE;
import static dev.langchain4j.data.message.ContentType.PDF;
import static dev.langchain4j.data.message.ContentType.VIDEO;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;

public final class GuardrailInspectionText {

  private static final String FILE_PLACEHOLDER = "<file: %s>";

  private GuardrailInspectionText() {}

  public static String text(UserMessage userMessage) {
    return Optional.ofNullable(userMessage)
        .map(UserMessage::contents)
        .orElseGet(List::of)
        .stream()
        .map(GuardrailInspectionText::describe)
        .filter(part -> !part.isEmpty())
        .collect(Collectors.joining("\n"));
  }

  private static String describe(Content content) {
    if (content instanceof TextContent text
        && Optional.ofNullable(text).map(TextContent::text).isPresent()) {
      return text.text();
    }

    return switch (content.type()) {
      case IMAGE -> filePlaceholder(IMAGE.name());
      case PDF -> filePlaceholder(PDF.name());
      case AUDIO -> filePlaceholder(AUDIO.name());
      case VIDEO -> filePlaceholder(VIDEO.name());
      default -> "";
    };
  }

  private static String filePlaceholder(String type) {
    return String.format(FILE_PLACEHOLDER, type);
  }
}
