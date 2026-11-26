package com.axonivy.utils.smart.workflow.model.gemini;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.axonivy.utils.smart.workflow.model.gemini.internal.GeminiReasoningEffortResolver;
import com.axonivy.utils.smart.workflow.model.gemini.internal.GeminiServiceConnector;
import com.axonivy.utils.smart.workflow.model.spi.ModelOptions;
import com.axonivy.utils.smart.workflow.model.spi.ReasoningEffort;

import ch.ivyteam.ivy.environment.IvyTest;
import ch.ivyteam.test.log.LoggerAccess;
import dev.langchain4j.model.googleai.GeminiThinkingConfig;

@IvyTest
class TestGeminiReasoningEffort {

  @RegisterExtension
  LoggerAccess log = new LoggerAccess();

  private final GeminiReasoningEffortResolver resolver = new GeminiReasoningEffortResolver();

  @ParameterizedTest
  @CsvSource({
      "gemini-2.5-flash, NONE, 0",
      "gemini-2.5-flash, LOW, 1024",
      "gemini-2.5-flash, MEDIUM, 8192",
      "gemini-2.5-flash, HIGH, 24576",
      "gemini-2.5-pro, LOW, 1024",
      "gemini-2.5-pro, MEDIUM, 8192",
      "gemini-2.5-pro, HIGH, 24576"
  })
  void budget(String model, ReasoningEffort effort, int expected) {
    assertThat(resolver.resolve(model, effort)).contains(thinkingBudget(expected));
    assertThat(log.warnings()).isEmpty();
  }

  @Test
  void budget_proCanNotStopThinking() {
    assertThat(resolver.resolve("gemini-2.5-pro", ReasoningEffort.NONE)).contains(thinkingBudget(128));
    assertThat(log.warnings()).containsExactly("Model 'gemini-2.5-pro' does not support reasoning effort NONE. "
        + "Using a budget of 128 tokens instead.");
  }

  @ParameterizedTest
  @ValueSource(strings = {"gemini-2.0-flash", "gemini-2.0-flash-exp", "gemini-1.5-pro", "gemini-1.5-flash",
      "gemini-3.6-flash", "unknown"})
  void notSupported(String model) {
    assertThat(resolver.resolve(model, ReasoningEffort.HIGH)).isEmpty();
    assertThat(log.warnings()).containsExactly(
        "Ignoring reasoning effort HIGH, because it is not supported for model '" + model + "'.");
  }

  @Test
  void notSet() {
    assertThat(resolver.resolve("gemini-2.5-flash", null)).isEmpty();
  }

  @Test
  void request() {
    var builder = GeminiServiceConnector.buildGeminiModel(ModelOptions.builder()
        .modelName("gemini-2.5-flash")
        .reasoningEffort(ReasoningEffort.LOW)
        .build());
    assertThat(builder).extracting("thinkingConfig").isEqualTo(thinkingBudget(1024));
    assertThat(builder).extracting("returnThinking", "sendThinking")
        .as("thought signatures are sent back with tool results")
        .containsExactly(true, true);
  }

  private static GeminiThinkingConfig thinkingBudget(int tokens) {
    return GeminiThinkingConfig.builder().thinkingBudget(tokens).build();
  }
}
