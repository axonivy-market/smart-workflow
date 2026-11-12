package com.axonivy.utils.smart.workflow.model.aura;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.axonivy.utils.smart.workflow.model.aura.internal.AuraInferenceProtocol;
import com.axonivy.utils.smart.workflow.utils.JsonUtils;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.Capability;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ResponseFormat;
import dev.langchain4j.model.chat.request.ResponseFormatType;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.request.json.JsonSchema;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.FinishReason;
import dev.langchain4j.service.AiServices;
import tools.jackson.databind.JsonNode;

/** Contract-level tests with synthetic fixtures; no live Aura requests. */
class TestAuraInferenceProtocol {

  @Test
  void textAndHistory() {
    var request = ChatRequest.builder().messages(
        SystemMessage.from("Answer briefly"),
        UserMessage.from("Hello"),
        AiMessage.from("Welcome"),
        UserMessage.from("Summarize this"))
        .temperature(0.4).maxOutputTokens(512).build();

    var body = AuraInferenceProtocol.request(request);

    assertThat(body.path("system_prompt").asString()).isEqualTo("Answer briefly");
    assertThat(body.path("messages").size()).isEqualTo(3);
    assertThat(body.at("/messages/1/role").asString()).isEqualTo("assistant");
    assertThat(body.at("/messages/2/content").asString()).isEqualTo("Summarize this");
    assertThat(body.path("temperature").asDouble()).isEqualTo(0.4);
    assertThat(body.path("max_completion_tokens").asInt()).isEqualTo(512);
    assertThat(body.path("use_history").asBoolean()).isFalse();
    assertThat(body.path("stream").asBoolean()).isFalse();
    assertThat(body.has("session_id")).isFalse();
    assertThat(body.has("model")).isFalse();
    assertThat(body.has("tools")).isFalse();
    assertThat(body.has("response_format")).isFalse();
  }

  @Test
  void optionalParametersAreNotInvented() {
    var body = AuraInferenceProtocol.request(query().build());
    assertThat(body.has("temperature")).isFalse();
    assertThat(body.has("max_completion_tokens")).isFalse();
    assertThat(body.has("system_prompt")).isFalse();
  }

  @Test
  void structuredOutput() {
    var schema = JsonObjectSchema.builder()
        .addStringProperty("name")
        .required("name")
        .additionalProperties(false)
        .build();
    var request = query().responseFormat(ResponseFormat.builder()
        .type(ResponseFormatType.JSON)
        .jsonSchema(JsonSchema.builder().name("Person").rootElement(schema).build())
        .build()).build();

    var body = AuraInferenceProtocol.request(request);

    assertThat(body.at("/response_format/type").asString()).isEqualTo("json_schema");
    assertThat(body.at("/response_format/json_schema/name").asString()).isEqualTo("Person");
    assertThat(body.at("/response_format/json_schema/schema/type").asString()).isEqualTo("object");
    assertThat(body.at("/response_format/json_schema/schema/properties/name/type").asString()).isEqualTo("string");
    assertThat(body.at("/response_format/json_schema/schema/required/0").asString()).isEqualTo("name");
    assertThat(body.at("/response_format/json_schema/schema/additionalProperties").asBoolean()).isFalse();
  }

  @Test
  void typedJavaOutput() {
    // Contract-shaped fixture, not a recorded live Aura response.
    ChatModel model = new ChatModel() {
      @Override
      public Set<Capability> supportedCapabilities() {
        return Set.of(Capability.RESPONSE_FORMAT_JSON_SCHEMA);
      }

      @Override
      public ChatResponse doChat(ChatRequest request) {
        var body = AuraInferenceProtocol.request(request);
        assertThat(body.at("/response_format/json_schema/schema/properties/name/type").asString()).isEqualTo("string");
        return AuraInferenceProtocol.response(fixture("structured-response.json"));
      }
    };
    var agent = AiServices.create(PersonAgent.class, model);

    assertThat(agent.extract("The person's name is Alex").name()).isEqualTo("Alex");
  }

  interface PersonAgent {
    Person extract(String message);
  }

  record Person(String name) {
  }

  @Test
  void textResponseDoesNotInventModelOrTokenUsage() {
    var response = AuraInferenceProtocol.response(fixture("response.json"));

    assertThat(response.aiMessage().text()).isEqualTo("Hello from Aura");
    assertThat(response.finishReason()).isEqualTo(FinishReason.STOP);
    assertThat(response.modelName()).isNull();
    assertThat(response.tokenUsage()).isNull();
  }

  @Test
  void preservesTruncationReason() {
    var response = JsonUtils.getObjectMapper().createObjectNode()
        .put("content", "Partial answer").put("finish_reason", "length");
    assertThat(AuraInferenceProtocol.response(response).finishReason()).isEqualTo(FinishReason.LENGTH);
  }

  @Test
  void contentFilterIsAnExplicitFailure() {
    var response = JsonUtils.getObjectMapper().createObjectNode()
        .put("content", "").put("finish_reason", "content_filter");
    assertThatThrownBy(() -> AuraInferenceProtocol.response(response))
        .isInstanceOf(IllegalStateException.class).hasMessageContaining("content filter");
  }

  @ParameterizedTest
  @ValueSource(strings = {"{}", "null", "[]", "{\"content\":42}", "{\"content\":\"\"}",
      "{\"content\":\"  \"}", "{\"job_id\":\"queued-job\",\"status\":\"queued\"}"})
  void rejectsIncompleteResponses(String json) {
    assertThatThrownBy(() -> AuraInferenceProtocol.response(JsonUtils.getObjectMapper().readTree(json)))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @ParameterizedTest
  @ValueSource(strings = {"gpt-4.1-mini", "unsupported-model"})
  void rejectsModelSelection(String model) {
    assertThatThrownBy(() -> AuraInferenceProtocol.request(query().modelName(model).build()))
        .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("leave Model empty");
  }

  @Test
  void rejectsTools() {
    var tool = ToolSpecification.builder().name("whoami").build();
    assertThatThrownBy(() -> AuraInferenceProtocol.request(query().toolSpecifications(tool).build()))
        .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Available tools empty");
  }

  @Test
  void rejectsToolMessages() {
    List<ChatMessage> messages = List.of(
        ToolExecutionResultMessage.from("call-1", "whoami", "Alex"),
        AiMessage.from(ToolExecutionRequest.builder().id("call-1").name("whoami").arguments("{}").build()));
    for (var message : messages) {
      assertThatThrownBy(() -> AuraInferenceProtocol.request(ChatRequest.builder().messages(message).build()))
          .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("tool calls or tool-result");
    }
  }

  @Test
  void rejectsFileInputs() {
    var message = UserMessage.from(ImageContent.from("https://example.invalid/image.png"));
    assertThatThrownBy(() -> AuraInferenceProtocol.request(ChatRequest.builder().messages(message).build()))
        .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("text input only");
  }

  @Test
  void rejectsJsonModeWithoutSchema() {
    assertThatThrownBy(() -> AuraInferenceProtocol.request(query().responseFormat(ResponseFormat.JSON).build()))
        .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("named JSON schema");
  }

  @ParameterizedTest
  @ValueSource(doubles = {-0.1, 2.1, Double.NaN, Double.POSITIVE_INFINITY})
  void rejectsInvalidTemperature(double temperature) {
    assertThatThrownBy(() -> AuraInferenceProtocol.request(query().temperature(temperature).build()))
        .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("temperature");
  }

  @ParameterizedTest
  @ValueSource(ints = {0, -1, 20001})
  void rejectsInvalidTokenLimit(int maxTokens) {
    assertThatThrownBy(() -> AuraInferenceProtocol.request(query().maxOutputTokens(maxTokens).build()))
        .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("max output tokens");
  }

  @Test
  void rejectsUnsupportedParameters() {
    assertThatThrownBy(() -> AuraInferenceProtocol.request(query().topP(0.9).build()))
        .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("topP");
  }

  @Test
  void rejectsMultipleSystemMessages() {
    var request = ChatRequest.builder().messages(
        SystemMessage.from("First"), SystemMessage.from("Second"), UserMessage.from("Hello")).build();
    assertThatThrownBy(() -> AuraInferenceProtocol.request(request))
        .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("one system message");
  }

  @Test
  void rejectsOversizedSystemMessage() {
    var request = ChatRequest.builder().messages(SystemMessage.from("x".repeat(10001)), UserMessage.from("Hello")).build();
    assertThatThrownBy(() -> AuraInferenceProtocol.request(request))
        .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("10000 characters");
  }

  @Test
  void requiresNonSystemMessages() {
    var request = ChatRequest.builder().messages(SystemMessage.from("Hello")).build();
    assertThatThrownBy(() -> AuraInferenceProtocol.request(request))
        .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("between 1 and 50");
  }

  @Test
  void rejectsTooManyMessages() {
    List<ChatMessage> messages = java.util.Collections.nCopies(51, UserMessage.from("Hello"));
    var request = ChatRequest.builder().messages(messages).build();
    assertThatThrownBy(() -> AuraInferenceProtocol.request(request))
        .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("between 1 and 50");
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"", " "})
  void doesNotEchoMalformedResponseContent(String content) {
    var response = JsonUtils.getObjectMapper().createObjectNode().put("content", content).put("secret", "do-not-echo");
    assertThatThrownBy(() -> AuraInferenceProtocol.response(response))
        .isInstanceOf(IllegalArgumentException.class).hasMessageNotContaining("do-not-echo");
  }

  private static ChatRequest.Builder query() {
    return ChatRequest.builder().messages(UserMessage.from("Hello"));
  }

  private static JsonNode fixture(String name) {
    try (var stream = TestAuraInferenceProtocol.class.getResourceAsStream(name)) {
      return JsonUtils.getObjectMapper().readTree(stream);
    } catch (IOException ex) {
      throw new IllegalStateException("Failed to read Aura test fixture", ex);
    }
  }
}
