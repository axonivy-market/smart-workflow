package com.axonivy.utils.smart.workflow.model.anthropic;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.axonivy.utils.smart.workflow.model.anthropic.internal.AnthropicServiceConnector.AnthropicConf;
import com.axonivy.utils.smart.workflow.model.spi.ModelOptions;
import com.axonivy.utils.smart.workflow.model.spi.ReasoningEffort;

import ch.ivyteam.ivy.environment.AppFixture;
import ch.ivyteam.ivy.environment.IvyTest;
import ch.ivyteam.test.log.LoggerAccess;
import dev.langchain4j.model.chat.request.ChatRequestParameters;

@IvyTest
class TestAnthropicReasoningEffort {

  @RegisterExtension
  LoggerAccess log = new LoggerAccess();

  @BeforeEach
  void setup(AppFixture fixture) {
    fixture.var(AnthropicConf.API_KEY, "test-key");
  }

  @ParameterizedTest
  @EnumSource
  void notSupported(ReasoningEffort effort) {
    assertThat(request(effort)).isEqualTo(request(null));
    assertThat(log.warnings()).containsExactly(
        "Ignoring reasoning effort " + effort + ", because it is not supported for Anthropic.");
  }

  @Test
  void notSet() {
    request(null);
    assertThat(log.warnings()).isEmpty();
  }

  private static ChatRequestParameters request(ReasoningEffort effort) {
    return new AnthropicModelProvider().setup(ModelOptions.builder()
        .modelName("claude-sonnet-4-6")
        .temperature(0.5)
        .maxOutputTokens(512)
        .reasoningEffort(effort)
        .build())
        .defaultRequestParameters();
  }
}
