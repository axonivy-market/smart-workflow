package com.axonivy.utils.smart.workflow.model.azureopenai;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.axonivy.utils.smart.workflow.azureopenai.internal.AzureOpenAiServiceConnector.AzureOpenAiConf;
import com.axonivy.utils.smart.workflow.model.ChatModelFactory;
import com.axonivy.utils.smart.workflow.model.spi.ChatModelProvider;
import com.axonivy.utils.smart.workflow.model.spi.ChatModelProvider.ModelOptions;
import com.axonivy.utils.smart.workflow.model.spi.internal.SpiLoader;

import ch.ivyteam.ivy.application.IProcessModelVersion;
import ch.ivyteam.ivy.environment.AppFixture;
import ch.ivyteam.ivy.environment.IvyTest;
import ch.ivyteam.ivy.project.model.Project;
import dev.langchain4j.model.chat.Capability;
import dev.langchain4j.model.chat.ChatModel;

@IvyTest
public class TestAzureOpenAiLoader {

  private static String TEST_DEPLOYMENT_NAME = "test-gpt-4-1-mini";
  private static String TEST_DEPLOYMENTS = """
      test-gpt-4-1-mini:
        Model: gpt-4.1-mini
        #[password]
        APIKey: ${decrypt:test}
      test-o4-mini:
        Model: o4-mini
        #[password]
        APIKey: ${decrypt:test}
      test-gpt-5-mini:
        Model: gpt-5-mini
        #[password]
        APIKey: ${decrypt:test}
      """;

  private ChatModelProvider provider;

  @BeforeEach
  void setup(AppFixture fixture) {
    fixture.var("AI.DefaultProvider", "AzureOpenAI");
    fixture.var(AzureOpenAiConf.ENDPOINT, "https://test.localhost.com");
    fixture.var(AzureOpenAiConf.DEPLOYMENTS, TEST_DEPLOYMENTS);
    provider = loadModel();
  }

  @Test
  void load() {
    Project project = IProcessModelVersion.current().project();
    var impls = new SpiLoader(project).load(ChatModelProvider.class);
    assertThat(impls).isNotEmpty();
    var azureModel = impls.stream().filter(p -> (p instanceof AzureOpenAiModelProvider)).toList();
    assertThat(azureModel).as("SPI loader finds Azure OpenAI implementor").isNotEmpty();
  }

  @Test
  void resolveByName() {
    assertThat(ChatModelFactory.create(AzureOpenAiModelProvider.NAME)).isPresent();
  }

  @Test
  void resolveBase() {
    assertThat(ChatModelFactory.create(AzureOpenAiModelProvider.NAME)).isNotEmpty();
  }

  @Test
  void listDeployments() {
    assertThat(provider.models()).contains(TEST_DEPLOYMENT_NAME);
  }

  @Test
  void capabilities() {
    ChatModel normal = provider.setup(new ModelOptions(TEST_DEPLOYMENT_NAME, false));
    assertThat(normal.supportedCapabilities()).isEmpty();

    ChatModel structured = provider.setup(new ModelOptions(TEST_DEPLOYMENT_NAME, true));
    assertThat(structured.supportedCapabilities()).contains(Capability.RESPONSE_FORMAT_JSON_SCHEMA);
  }

  private static ChatModelProvider loadModel() {
    return ChatModelFactory.create(AzureOpenAiModelProvider.NAME).get();
  }
}
