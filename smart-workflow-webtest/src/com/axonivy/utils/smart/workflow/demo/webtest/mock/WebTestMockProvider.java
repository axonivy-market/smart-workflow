package com.axonivy.utils.smart.workflow.demo.webtest.mock;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.smart.workflow.model.spi.ChatModelProvider;
import com.axonivy.utils.smart.workflow.model.spi.ModelOptions;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.exception.AuthenticationException;
import dev.langchain4j.exception.InternalServerException;
import dev.langchain4j.exception.RateLimitException;
import dev.langchain4j.model.chat.Capability;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.model.chat.request.ResponseFormat;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.request.json.JsonSchema;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.FinishReason;
import dev.langchain4j.model.output.TokenUsage;

/**
 * Model provider for web tests. It answers inside the engine, so no request ever reaches a real model, and it
 * answers with the {@link WebTestMockRule rules} the running test defined. A request no rule matches fails.
 */
public class WebTestMockProvider implements ChatModelProvider {

  public static final String NAME = "WebTestMock";

  private static final String MODEL = "webtest-mock";

  @Override
  public String name() {
    return NAME;
  }

  @Override
  public List<String> models() {
    return List.of(MODEL);
  }

  @Override
  public List<String> secretsVars() {
    return List.of();
  }

  @Override
  public ChatModel setup(ModelOptions options) {
    return new WebTestMockModel(options);
  }

  private static class WebTestMockModel implements ChatModel {

    private final ModelOptions options;

    WebTestMockModel(ModelOptions options) {
      this.options = options;
    }

    @Override
    public List<ChatModelListener> listeners() {
      return options.listeners();
    }

    @Override
    public Set<Capability> supportedCapabilities() {
      // structured output then arrives as a response format, whose schema fields a rule can match
      return options.structuredOutput() ? Set.of(Capability.RESPONSE_FORMAT_JSON_SCHEMA) : Set.of();
    }

    @Override
    public ChatRequestParameters defaultRequestParameters() {
      return ChatRequestParameters.builder()
          .modelName(StringUtils.defaultIfBlank(options.modelName(), MODEL))
          .build();
    }

    @Override
    public ChatResponse doChat(ChatRequest request) {
      var rules = WebTestMockRules.current();
      var searchable = searchableText(request);
      var toolResult = lastToolResult(request);
      return rules.stream()
          .filter(rule -> rule.requestContains() == null || searchable.contains(rule.requestContains()))
          .filter(rule -> rule.toolResult() == null || rule.toolResult() == toolResult.isPresent())
          .findFirst()
          .map(rule -> respond(rule, toolResult.orElse("")))
          .orElseThrow(() -> new IllegalStateException(
              NAME + " has no rule for this request. Rules: " + rules + ". Request: " + request));
    }

    private static ChatResponse respond(WebTestMockRule rule, String toolResult) {
      if (rule.failure() != null) {
        throw failure(rule.failure());
      }
      if (rule.toolName() != null) {
        var call = ToolExecutionRequest.builder()
            .id("call_" + rule.toolName())
            .name(rule.toolName())
            .arguments(fill(StringUtils.defaultIfBlank(rule.toolArguments(), "{}"), toolResult))
            .build();
        return response(AiMessage.from(call), FinishReason.TOOL_EXECUTION);
      }
      return response(AiMessage.from(fill(StringUtils.defaultString(rule.answer()), toolResult)), FinishReason.STOP);
    }

    private static RuntimeException failure(WebTestMockRule.Failure kind) {
      var mocked = " (mocked by " + NAME + ")";
      return switch (kind) {
        case INVALID_API_KEY -> new AuthenticationException("Incorrect API key provided: sk-wrong." + mocked);
        case RATE_LIMIT -> new RateLimitException("Rate limit reached for requests." + mocked);
        case SERVER_ERROR -> new InternalServerException("The server had an error while processing your request." + mocked);
      };
    }

    private static String fill(String text, String toolResult) {
      return text
          .replace(WebTestMockRule.TOOL_RESULT, toolResult)
          .replace(WebTestMockRule.UNIQUE_ID, UUID.randomUUID().toString());
    }

    /** messages, offered tool names and expected result fields: everything a rule may match */
    private static String searchableText(ChatRequest request) {
      var messages = request.messages().stream().map(String::valueOf);
      var tools = Optional.ofNullable(request.toolSpecifications()).orElse(List.of()).stream()
          .map(ToolSpecification::name);
      return Stream.of(messages, tools, schemaFields(request).stream())
          .flatMap(s -> s)
          .filter(Objects::nonNull)
          .collect(Collectors.joining("\n"));
    }

    private static Optional<String> lastToolResult(ChatRequest request) {
      List<ChatMessage> messages = request.messages();
      for (int i = messages.size() - 1; i >= 0; i--) {
        if (messages.get(i) instanceof ToolExecutionResultMessage result) {
          return Optional.of(result.text());
        }
      }
      return Optional.empty();
    }

    private static Set<String> schemaFields(ChatRequest request) {
      return Optional.ofNullable(request.responseFormat())
          .map(ResponseFormat::jsonSchema)
          .map(JsonSchema::rootElement)
          .filter(JsonObjectSchema.class::isInstance)
          .map(root -> ((JsonObjectSchema) root).properties().keySet())
          .orElse(Set.of());
    }

    private static ChatResponse response(AiMessage message, FinishReason finishReason) {
      return ChatResponse.builder()
          .aiMessage(message)
          .modelName(MODEL)
          .tokenUsage(new TokenUsage(10, 10))
          .finishReason(finishReason)
          .build();
    }
  }
}
