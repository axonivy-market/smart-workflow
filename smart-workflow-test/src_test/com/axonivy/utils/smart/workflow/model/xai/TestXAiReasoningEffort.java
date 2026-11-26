package com.axonivy.utils.smart.workflow.model.xai;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.axonivy.utils.smart.workflow.model.spi.ModelOptions;
import com.axonivy.utils.smart.workflow.model.spi.ReasoningEffort;
import com.axonivy.utils.smart.workflow.model.xai.internal.XAiReasoningEffortResolver;
import com.axonivy.utils.smart.workflow.model.xai.internal.XAiServiceConnector;

import ch.ivyteam.ivy.environment.IvyTest;
import ch.ivyteam.test.log.LoggerAccess;

@IvyTest
class TestXAiReasoningEffort {

  @RegisterExtension
  LoggerAccess log = new LoggerAccess();

  private final XAiReasoningEffortResolver resolver = new XAiReasoningEffortResolver();

  @ParameterizedTest
  @CsvSource({
      "grok-4.3, NONE, none",
      "grok-4.3, LOW, low",
      "grok-4.3, MEDIUM, medium",
      "grok-4.3, HIGH, high",
      "grok-4.7, NONE, low",
      "grok-4.7, HIGH, high",
      "grok-4.20-0309-reasoning, LOW, low",
      "grok-4-1-fast, NONE, low",
      "grok-4-1-fast, MEDIUM, medium"
  })
  void effort(String model, ReasoningEffort effort, String expected) {
    assertThat(resolver.resolve(model, effort)).contains(expected);
  }

  @Test
  void none_usesLowInstead() {
    assertThat(resolver.resolve("grok-4.7", ReasoningEffort.NONE)).contains("low");
    assertThat(log.warnings()).containsExactly(
        "Model 'grok-4.7' does not support reasoning effort NONE. Using low instead.");
  }

  @ParameterizedTest
  @ValueSource(strings = {"grok-4.20-0309-non-reasoning", "GROK-4.20-0309-NON-REASONING"})
  void notSupported(String model) {
    assertThat(resolver.resolve(model, ReasoningEffort.HIGH)).isEmpty();
    assertThat(log.warnings()).containsExactly(
        "Ignoring reasoning effort HIGH, because it is not supported for model '" + model + "'.");
  }

  @Test
  void notSet() {
    assertThat(resolver.resolve("grok-4.3", null)).isEmpty();
  }

  @Test
  void request() {
    var request = XAiServiceConnector.buildOpenAiModel(ModelOptions.builder()
        .modelName("grok-4.3")
        .temperature(0.7)
        .reasoningEffort(ReasoningEffort.MEDIUM)
        .build())
        .build()
        .defaultRequestParameters();
    assertThat(request.reasoningEffort()).isEqualTo("medium");
    assertThat(request.temperature()).isEqualTo(0.7);
  }

  @Test
  void request_notSupported() {
    var request = XAiServiceConnector.buildOpenAiModel(ModelOptions.builder()
        .modelName("grok-4.20-0309-non-reasoning")
        .reasoningEffort(ReasoningEffort.MEDIUM)
        .build())
        .build()
        .defaultRequestParameters();
    assertThat(request.reasoningEffort()).isNull();
  }
}
