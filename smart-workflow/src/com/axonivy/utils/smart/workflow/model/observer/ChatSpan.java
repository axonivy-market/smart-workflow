package com.axonivy.utils.smart.workflow.model.observer;

import java.util.ArrayList;
import java.util.List;

import ch.ivyteam.ivy.trace.Attribute;
import ch.ivyteam.ivy.trace.Span;
import ch.ivyteam.ivy.trace.SpanInstance;
import ch.ivyteam.ivy.trace.SpanResult;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.listener.ChatModelErrorContext;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;

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
    attributes.add(Attribute.attribute("gen_ai.request.model", getRequestModelName(requestContext)));
    attributes.add(Attribute.attribute("gen_ai.tool.definitions", requestContext.chatRequest().toolSpecifications()));
    attributes.add(Attribute.attribute("openinference.span.kind", "LLM"));

    for(int index = 0; index < requestContext.chatRequest().messages().size(); index++) {
      var message = requestContext.chatRequest().messages().get(index);
      attributes.add(Attribute.attribute("llm.input_messages." + index + ".message.role", message.type().toString()));
      attributes.add(Attribute.attribute("llm.input_messages." + index + ".message.content", rawMessageOf(message)));
    }

    attributes.add(Attribute.attribute("llm.model_name", "gpt-4-turbo-preview"));
    
    this.span = Span.open(() -> this);
  }

  private static String rawMessageOf(ChatMessage message) {
    return switch (message.type()) {
      case SYSTEM -> ((SystemMessage) message).text();
      case USER -> ((UserMessage) message).contents().toString();
      default -> message.toString();
    };
  }

  @Override
  public void onResponse(ChatModelResponseContext responseContext) {
    var tokenUsage = responseContext.chatResponse().tokenUsage();
    attributes.add(Attribute.attribute("gen_ai.usage.input_tokens", tokenUsage.inputTokenCount()));
    attributes.add(Attribute.attribute("gen_ai.usage.output_tokens", tokenUsage.outputTokenCount()));
    attributes.add(Attribute.attribute("gen_ai.response.model", responseContext.chatResponse().modelName()));
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
