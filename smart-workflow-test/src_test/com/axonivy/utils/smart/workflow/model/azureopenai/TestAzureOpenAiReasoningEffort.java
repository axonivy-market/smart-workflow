package com.axonivy.utils.smart.workflow.model.azureopenai;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.axonivy.utils.smart.workflow.model.azureopenai.internal.AzureOpenAiConf;
import com.axonivy.utils.smart.workflow.model.spi.ModelOptions;
import com.axonivy.utils.smart.workflow.model.spi.ReasoningEffort;

import ch.ivyteam.ivy.environment.AppFixture;
import ch.ivyteam.ivy.environment.IvyTest;
import ch.ivyteam.test.log.LoggerAccess;
import dev.langchain4j.model.chat.request.ChatRequestParameters;

@IvyTest
class TestAzureOpenAiReasoningEffort {

  @RegisterExtension
  LoggerAccess log = new LoggerAccess();

  @BeforeEach
  void setup(AppFixture fixture) {
    fixture.var(AzureOpenAiConf.ENDPOINT, "https://localhost/");
    fixture.var(AzureOpenAiConf.DEPLOYMENTS + ".my-deployment.APIKey", "test-key");
    fixture.var(AzureOpenAiConf.DEPLOYMENTS + ".my-deployment.Model", "gpt-5.1");
  }

  @ParameterizedTest
  @EnumSource
  void notSupported(ReasoningEffort effort) {
    assertThat(request(effort)).isEqualTo(request(null));
    assertThat(log.warnings()).containsExactly(
        "Ignoring reasoning effort " + effort + ", because it is not supported for AzureOpenAI.");
  }

  @Test
  void notSet() {
    request(null);
    assertThat(log.warnings()).isEmpty();
  }

  private static ChatRequestParameters request(ReasoningEffort effort) {
    return new AzureOpenAiModelProvider().setup(ModelOptions.builder()
        .modelName("my-deployment")
        .temperature(0.5)
        .maxOutputTokens(512)
        .reasoningEffort(effort)
        .build())
        .defaultRequestParameters();
  }
}
