package com.axonivy.utils.smart.workflow.model.observer;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import ch.ivyteam.ivy.trace.Attribute;
import ch.ivyteam.ivy.trace.Span;
import ch.ivyteam.ivy.trace.SpanInstance;
import ch.ivyteam.ivy.trace.SpanResult;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.listener.ChatModelErrorContext;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;

import static ch.ivyteam.ivy.trace.Attribute.attribute;

public class ChatSpan implements SpanInstance<Void>, ChatModelListener {

  private final List<Attribute> attributes = new ArrayList<>();
  private Span<Void> span;

  @Override
  public String name() {
    return "AI Assistant";
  }

  @Override
  public List<Attribute> attributes() {
    return attributes;
  }

  @Override
  public SpanResult result(Void result) {
    return SpanResult.ok(attributes().toArray(Attribute[]::new));
  }

  @Override
  public void onRequest(ChatModelRequestContext requestContext) {
    attributes.add(attribute("gen_ai.request.model", getRequestModelName(requestContext)));
    attributes.add(attribute("gen_ai.tool.definitions", requestContext.chatRequest().toolSpecifications()));
    attributes.add(attribute("openinference.span.kind", "LLM"));
    // TOOL
    // CHAIN
    // LLM
    // RETRIEVER
    // EMBEDDING
    // AGENT
    // RERANKER
    // UNKNOWN
    // GUARDRAIL
    // EVALUATOR
    // PROMPT

    for(int index = 0; index < requestContext.chatRequest().messages().size(); index++) {
      var message = requestContext.chatRequest().messages().get(index);
      attributes.add(attribute("llm.input_messages." + index + ".message.role", message.type().toString()));
      attributes.add(attribute("llm.input_messages." + index + ".message.content", renderMessage(message)));
    }

    attributes.add(attribute("llm.model_name", "gpt-4-turbo-preview"));
    
    this.span = Span.open(() -> this);
  }

  private static String renderMessage(ChatMessage message) {
    return switch (message) {
      case SystemMessage systemMessage -> systemMessage.text();
      case UserMessage userMessage -> renderContents(userMessage.contents());
      default -> message.toString();
    };
  }

  private static String renderContents(List<Content> contents) {
    var prompt = new StringBuilder();
    contents.stream().forEach(c -> {
      if (c instanceof TextContent text) {
        prompt.append(text.text());
        return;
      }
      prompt.append(c.toString());
    });
    return prompt.toString();
  }

  @Override
  public void onResponse(ChatModelResponseContext responseContext) {
    var tokenUsage = responseContext.chatResponse().tokenUsage();
    // support more:
    // https://github.com/Arize-ai/openinference/blob/main/python/openinference-semantic-conventions/src/openinference/semconv/trace/__init__.py
    attributes.add(attribute("gen_ai.usage.input_tokens", tokenUsage.inputTokenCount()));
    attributes.add(attribute("gen_ai.usage.output_tokens", tokenUsage.outputTokenCount()));
    attributes.add(attribute("gen_ai.response.model", responseContext.chatResponse().modelName()));
    
    // cost reporting 4 phoenix
    attributes.add(attribute("llm.token_count.total", tokenUsage.totalTokenCount()));


    Optional.ofNullable(responseContext.chatResponse().aiMessage()).ifPresent(aiMessage -> {
      attributes.add(attribute("llm.output_message.0.message.role", aiMessage.type().toString()));
      attributes.add(attribute("llm.output_message.0.message.content", renderMessage(aiMessage)));
    });

    this.result(null);
    this.span.close();
  }

  @Override
  public void onError(ChatModelErrorContext errorContext) {
    this.span.error(errorContext.error());
    this.span.close();
  }

  private static String getRequestModelName(ChatModelRequestContext requestContext) {
    return requestContext.chatRequest().parameters().modelName();
  }

}
