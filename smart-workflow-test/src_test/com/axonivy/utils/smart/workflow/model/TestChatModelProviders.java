package com.axonivy.utils.smart.workflow.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.axonivy.utils.smart.workflow.model.anthropic.internal.AnthropicServiceConnector.AnthropicConf;
import com.axonivy.utils.smart.workflow.model.azureopenai.internal.AzureOpenAiConf;
import com.axonivy.utils.smart.workflow.model.spi.ChatModelProvider;
import com.axonivy.utils.smart.workflow.model.spi.ChatModelProvider.ModelOptions;
import com.axonivy.utils.smart.workflow.spi.internal.SpiLoader;
import com.axonivy.utils.smart.workflow.utils.JsonUtils;
import com.fasterxml.jackson.databind.JsonNode;

import ch.ivyteam.ivy.environment.AppFixture;
import ch.ivyteam.ivy.environment.IvyTest;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;

@IvyTest
class TestChatModelProviders {

  private static final JsonNode PRODUCT = productJson();

  @BeforeEach
  void setUp(AppFixture fixture) {
    fixture.var(AnthropicConf.API_KEY, "notMyKey"); // anthropic fails with empty key
    fixture.var(Azure.DEPLOYMENTS_PREFIX + "." + Azure.TEST_DEPLOYMENT_NAME + "." + AzureOpenAiConf.MODEL_FIELD,
        Azure.MODEL);
    fixture.var(Azure.DEPLOYMENTS_PREFIX + "." + Azure.TEST_DEPLOYMENT_NAME + "." + AzureOpenAiConf.API_KEY_FIELD,
        Azure.API_KEY);
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

  @ParameterizedTest
  @MethodSource("providerNames")
  void installer_promotesProvider(String providerName) throws Exception {
    if (providerName.equals("dummy")) {
      return; // dummy provider is not actually installed, just for testing resolution
    }
    var provider = ChatModelFactory.create(providerName).orElseThrow();
    var artifactIds = installerArtifactIds(PRODUCT);
    assertThat(artifactIds)
      .as("provider is installable as extra market product")
      .contains(installerName(provider));
  }

  private static List<String> installerArtifactIds(JsonNode product) {
    return stream(product.path("installers"))
        .flatMap(installer -> stream(installer.path("data").path("projects")))
        .map(project -> project.path("artifactId").asText())
        .toList();
  }

  private static Stream<JsonNode> stream(JsonNode node) {
    return StreamSupport.stream(node.spliterator(), false);
  }

  private static JsonNode productJson() {
    try {
      var where = TestChatModelProviders.class.getResource("/").toURI();
      var repo = Path.of(where).getParent().getParent().getParent();
      var product = repo.resolve("smart-workflow-product").resolve("product.json");
      try(var in = Files.newInputStream(product, StandardOpenOption.READ)){
        return JsonUtils.getObjectMapper().readTree(in);
      }
    } catch (Exception e) {
      throw new RuntimeException("Failed to load product.json for test", e);
    }
  }

  private static String installerName(ChatModelProvider provider) {
    if (provider.name().equals("AzureOpenAI")) {
      return "smart-workflow-azure-openai";
    }
    return "smart-workflow-" + provider.name().toLowerCase(Locale.ENGLISH);
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
