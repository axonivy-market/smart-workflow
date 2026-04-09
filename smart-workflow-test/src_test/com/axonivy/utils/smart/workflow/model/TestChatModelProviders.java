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
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;

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
  void observable_sharesModelAsRequestParameter(String providerName) {
    var provider = ChatModelFactory.create(providerName).orElseThrow();
    var model = provider.setup(new ModelOptions("AwesomeModel", true, List.of()));
    assertThat(model.defaultRequestParameters().modelName())
      .as("Model as request parameter; allows tracing distribution for provider " + providerName)
      .isEqualTo("AwesomeModel");
  }

  @ParameterizedTest
  @MethodSource("providerNames")
  void listeners_areInstalled(String providerName) {
    var provider = ChatModelFactory.create(providerName).orElseThrow();
    var myListener = new ChatModelListener() {
      @Override
      public void onRequest(ChatModelRequestContext context) {
        System.out.println("request");
      }

      @Override
      public void onResponse(ChatModelResponseContext context) {
        System.out.println("response");
      }
    };
    var model = provider.setup(new ModelOptions("", true, List.of(myListener)));
    assertThat(model.listeners())
      .as("providers install listeners passed from Agent")
      .contains(myListener);
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
