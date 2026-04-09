package com.axonivy.utils.smart.workflow.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.axonivy.utils.smart.workflow.model.anthropic.internal.AnthropicServiceConnector.AnthropicConf;
import com.axonivy.utils.smart.workflow.model.azureopenai.internal.AzureOpenAiConf;
import com.axonivy.utils.smart.workflow.model.spi.ChatModelProvider;
import com.axonivy.utils.smart.workflow.model.spi.ChatModelProvider.ModelOptions;
import com.axonivy.utils.smart.workflow.spi.internal.SpiLoader;

import ch.ivyteam.ivy.environment.AppFixture;
import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
class TestChatModelProviders {

  @BeforeEach
  void setUp(AppFixture fixture) {
    fixture.var(AnthropicConf.API_KEY, "notMyKey"); // anthropic fails with empty key
    fixture.var(Azure.DEPLOYMENTS_PREFIX + "." + Azure.TEST_DEPLOYMENT_NAME + "." + AzureOpenAiConf.MODEL_FIELD, Azure.MODEL);
    fixture.var(Azure.DEPLOYMENTS_PREFIX + "." + Azure.TEST_DEPLOYMENT_NAME + "." + AzureOpenAiConf.API_KEY_FIELD, Azure.API_KEY);
  }

  @ParameterizedTest
  @MethodSource("providerNames")
  void observable_sharesModelAsRequestParameter(String providerName, AppFixture fixture) throws ClassNotFoundException {
    var provider = ChatModelFactory.create(providerName).orElseThrow();
    var model = provider.setup(new ModelOptions("AwesomeModel", true, List.of()));
    assertThat(model.defaultRequestParameters().modelName())
      .as("Model as request parameter; allows tracing distribution for provider " + providerName)
      .isEqualTo("AwesomeModel");
  }

  static Stream<String> providerNames() {
    var classLoader = TestChatModelProviders.class.getClassLoader();
    return SpiLoader.findImpl(ChatModelProvider.class, classLoader).stream()
      .map(ChatModelProvider::name);
  }

  private interface Azure {
    String DEPLOYMENTS_PREFIX = AzureOpenAiConf.DEPLOYMENTS;
    String TEST_DEPLOYMENT_NAME = "AwesomeModel";
    String MODEL = "AwesomeModel";
    String API_KEY = "${decrypt:test-key-1}";
  }
}
