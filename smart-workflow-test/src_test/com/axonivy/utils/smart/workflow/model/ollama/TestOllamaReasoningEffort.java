package com.axonivy.utils.smart.workflow.model.ollama;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.axonivy.utils.smart.workflow.model.ollama.internal.OllamaReasoningEffortResolver;
import com.axonivy.utils.smart.workflow.model.ollama.internal.OllamaServiceConnector;
import com.axonivy.utils.smart.workflow.model.spi.ModelOptions;
import com.axonivy.utils.smart.workflow.model.spi.ReasoningEffort;

import ch.ivyteam.ivy.environment.IvyTest;
import ch.ivyteam.test.log.LoggerAccess;

@IvyTest
class TestOllamaReasoningEffort {

  @RegisterExtension
  LoggerAccess log = new LoggerAccess();

  private final OllamaReasoningEffortResolver resolver = new OllamaReasoningEffortResolver();

  @ParameterizedTest
  @CsvSource({"NONE, false", "LOW, true", "MEDIUM, true", "HIGH, true"})
  void effort(ReasoningEffort effort, boolean expected) {
    assertThat(resolver.resolve("qwen3:30b", effort)).contains(expected);
    assertThat(resolver.resolve("gemma4", effort)).contains(expected);
    assertThat(log.warnings()).isEmpty();
  }

  @Test
  void alwaysThinks() {
    assertThat(resolver.resolve("gpt-oss:20b", ReasoningEffort.NONE)).contains(false);
    assertThat(log.warnings()).containsExactly(
        "Model 'gpt-oss:20b' does not support reasoning effort NONE. Using low instead.");
  }

  @ParameterizedTest
  @ValueSource(strings = {"llama3.2", "llama3.2:3b", "llama3", "llava", "llava:13b", "gemma3:27b", "gemma"})
  void notSupported(String model) {
    assertThat(resolver.resolve(model, ReasoningEffort.HIGH)).isEmpty();
  }

  @Test
  void notSet() {
    assertThat(resolver.resolve("qwen3", null)).isEmpty();
  }

  @ParameterizedTest
  @CsvSource({"qwen3, true", "llama3.2,"})
  void request(String model, Boolean expected) {
    var builder = OllamaServiceConnector.buildChatModel(ModelOptions.builder()
        .modelName(model)
        .reasoningEffort(ReasoningEffort.LOW)
        .build());
    assertThat(builder).extracting("think").isEqualTo(expected);
  }
}
