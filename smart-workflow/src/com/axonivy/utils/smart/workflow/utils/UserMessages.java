package com.axonivy.utils.smart.workflow.utils;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;

public final class UserMessages {

  private UserMessages() {}

  /**
   * Text a guardrail should inspect: all text parts joined, images and other media ignored.
   *
   * <p>Deliberately not {@link UserMessage#singleText()}, which throws on a multimodal message such as
   * "describe this: &lt;image&gt;". Deliberately no placeholder for the skipped media either: a rewriting
   * guardrail feeds this string back into the prompt via langchain4j's {@code rewriteUserMessage}, so any
   * marker would end up in the message sent to the model.
   *
   * <p>Note that {@code rewriteUserMessage} replaces <em>every</em> text part with the returned string. That
   * round-trips exactly for the usual single-text-part message; a query with several expressions produces
   * several text parts, and a rewriting guardrail would then repeat the joined text in each.
   *
   * @return the joined text, or an empty string when the message is null or carries no text
   */
  public static String text(UserMessage userMessage) {
    return Optional.ofNullable(userMessage)
        .map(UserMessage::contents)
        .orElseGet(List::of)
        .stream()
        .filter(TextContent.class::isInstance)
        .map(TextContent.class::cast)
        .map(TextContent::text)
        .collect(Collectors.joining("\n"));
  }
}
