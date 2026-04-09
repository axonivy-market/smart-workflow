package com.axonivy.utils.smart.workflow.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.axonivy.utils.smart.workflow.model.anthropic.internal.AnthropicServiceConnector.AnthropicConf;
import com.axonivy.utils.smart.workflow.model.azureopenai.internal.AzureOpenAiConf;
import com.axonivy.utils.smart.workflow.model.spi.ChatModelProvider.ModelOptions;

import ch.ivyteam.ivy.environment.AppFixture;
import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
class TestChatModelProviders {

  @Test
  void observable_sharesModelAsRequestParameter(AppFixture fixture) {
    fixture.var(AnthropicConf.API_KEY, "notMyKey"); // anthropic fails with empty key
    fixture.var(Azure.DEPLOYMENTS_PREFIX + "." + Azure.TEST_DEPLOYMENT_NAME + "." + AzureOpenAiConf.MODEL_FIELD, Azure.MODEL);
    fixture.var(Azure.DEPLOYMENTS_PREFIX + "." + Azure.TEST_DEPLOYMENT_NAME + "." + AzureOpenAiConf.API_KEY_FIELD, Azure.API_KEY);

    ChatModelFactory.providers().forEach(p -> {
      var model = p.setup(new ModelOptions("AwesomeModel", true, List.of()));
      assertThat(model.defaultRequestParameters().modelName())
        .as("Model as request parameter; allows tracing distribution for provider "+p.name())
        .isEqualTo("AwesomeModel");
    });
  }

  private interface Azure {
    String DEPLOYMENTS_PREFIX = AzureOpenAiConf.DEPLOYMENTS;
    String TEST_DEPLOYMENT_NAME = "AwesomeModel";
    String MODEL = "AwesomeModel";
    String API_KEY = "${decrypt:test-key-1}";
  }
}