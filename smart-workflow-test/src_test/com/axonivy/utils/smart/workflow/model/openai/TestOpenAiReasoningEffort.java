package com.axonivy.utils.smart.workflow.model.openai;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.axonivy.utils.smart.workflow.model.openai.internal.OpenAiReasoningEffortResolver;
import com.axonivy.utils.smart.workflow.model.openai.internal.OpenAiServiceConnector;
import com.axonivy.utils.smart.workflow.model.spi.ModelOptions;
import com.axonivy.utils.smart.workflow.model.spi.ReasoningEffort;

import ch.ivyteam.ivy.environment.IvyTest;
import ch.ivyteam.test.log.LoggerAccess;
import dev.langchain4j.model.openai.OpenAiChatRequestParameters;

@IvyTest
class TestOpenAiReasoningEffort {

  @RegisterExtension
  LoggerAccess log = new LoggerAccess();

  private final OpenAiReasoningEffortResolver resolver = new OpenAiReasoningEffortResolver();

  @ParameterizedTest
  @CsvSource({"LOW, low", "MEDIUM, medium", "HIGH, high"})
  void effort(ReasoningEffort effort, String expected) {
    assertThat(resolver.resolve("gpt-5", effort)).contains(expected);
    assertThat(resolver.resolve("gpt-5.1", effort)).contains(expected);
    assertThat(resolver.resolve("gpt-6.2-sol", effort)).contains(expected);
    assertThat(resolver.resolve("o3", effort)).contains(expected);
  }

  @ParameterizedTest
  @ValueSource(strings = {"gpt-5.1", "gpt-5.1-2025-11-13", "gpt-5.2", "gpt-5.4", "gpt-5.5", "gpt-5.6-sol", "gpt-6-sol",
      "gpt-6-luna", "gpt-6.1-terra"})
  void none(String model) {
    assertThat(resolver.resolve(model, ReasoningEffort.NONE)).contains("none");
    assertThat(log.warnings()).isEmpty();
  }

  @ParameterizedTest
  @CsvSource({"gpt-5, minimal", "gpt-5-2025-08-07, minimal", "gpt-5-mini, minimal", "gpt-5-nano, minimal",
      "o3, low", "o4-mini, low", "gpt-5-pro, high",
      "gpt-6-astra, low", "gpt-6.1-sol, low", "gpt-6.2-sol, low", "unknown, low"})
  void none_usesLowestInstead(String model, String expected) {
    assertThat(resolver.resolve(model, ReasoningEffort.NONE)).contains(expected);
    assertThat(log.warnings()).containsExactly("Model '" + model + "' does not support reasoning effort NONE. Using "
        + expected + " instead.");
  }

  @ParameterizedTest
  @ValueSource(strings = {"gpt-3.5-turbo", "gpt-4", "gpt-4o", "gpt-4o-mini", "gpt-4.1", "gpt-4.1-mini-2025-04-14"})
  void notSupported(String model) {
    assertThat(resolver.resolve(model, ReasoningEffort.HIGH)).isEmpty();
    assertThat(log.warnings()).containsExactly(
        "Ignoring reasoning effort HIGH, because it is not supported for model '" + model + "'.");
  }

  @Test
  void notSupported_noneIsFine() {
    assertThat(resolver.resolve("gpt-4.1-mini", ReasoningEffort.NONE)).isEmpty();
    assertThat(log.warnings()).isEmpty();
  }

  @Test
  void notSet() {
    assertThat(resolver.resolve("gpt-5", null)).isEmpty();
    assertThat(log.warnings()).isEmpty();
  }

  @Test
  void request_reasoning() {
    var request = request("gpt-5.1", ReasoningEffort.LOW);
    assertThat(request.reasoningEffort()).isEqualTo("low");
    assertThat(request.temperature()).isNull();
    assertThat(request.maxOutputTokens()).isNull();
    assertThat(request.maxCompletionTokens()).isEqualTo(512);
  }

  @Test
  void request_none() {
    var request = request("gpt-5.1", ReasoningEffort.NONE);
    assertThat(request.reasoningEffort()).isEqualTo("none");
    assertThat(request.temperature()).isEqualTo(1.0);
    assertThat(request.maxOutputTokens()).isNull();
    assertThat(request.maxCompletionTokens()).isEqualTo(512);
  }

  @ParameterizedTest
  @EnumSource(ReasoningEffort.class)
  void request_notSupported(ReasoningEffort effort) {
    var request = request("gpt-4.1-mini", effort);
    assertThat(request).isEqualTo(request("gpt-4.1-mini", null));
    assertThat(request.reasoningEffort()).isNull();
    assertThat(request.temperature()).isEqualTo(0.5);
    assertThat(request.maxOutputTokens()).isEqualTo(512);
  }

  private static OpenAiChatRequestParameters request(String model, ReasoningEffort effort) {
    return OpenAiServiceConnector.buildOpenAiModel(ModelOptions.builder()
        .modelName(model)
        .temperature(0.5)
        .maxOutputTokens(512)
        .reasoningEffort(effort)
        .build())
        .build()
        .defaultRequestParameters();
  }
}
