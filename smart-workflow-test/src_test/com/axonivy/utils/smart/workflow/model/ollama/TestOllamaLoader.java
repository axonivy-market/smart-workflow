package com.axonivy.utils.smart.workflow.model.ollama;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.axonivy.utils.smart.workflow.model.ChatModelFactory;
import com.axonivy.utils.smart.workflow.model.ollama.internal.OllamaServiceConnector.OllamaConf;
import com.axonivy.utils.smart.workflow.model.spi.ChatModelProvider;
import com.axonivy.utils.smart.workflow.model.spi.ChatModelProvider.ModelOptions;

import ch.ivyteam.ivy.environment.AppFixture;
import ch.ivyteam.ivy.environment.IvyTest;
import dev.langchain4j.model.chat.Capability;
import dev.langchain4j.model.chat.ChatModel;

@IvyTest
public class TestOllamaLoader {

  private static final String MODEL = "llama3.2";

  private ChatModelProvider provider;

  @BeforeEach
  void setup(AppFixture fixture) {
    fixture.var(OllamaConf.DEFAULT_MODEL, MODEL);
    provider = loadProvider();
  }

  @Test
  void load() {
    assertThat(provider).isNotNull();
    assertThat(provider.name()).isEqualTo(OllamaModelProvider.NAME);
  }

  @Test
  void resolveByName() {
    assertThat(ChatModelFactory.create(OllamaModelProvider.NAME)).isPresent();
  }

  @Test
  void listModels() {
    assertThat(provider.models()).isEmpty();
  }

  @Test
  void modelNameIsPassedAsRequestParameter() {
    ChatModel model = provider.setup(new ModelOptions(MODEL, false, List.of()));
    assertThat(model.defaultRequestParameters().modelName()).isEqualTo(MODEL);
  }

  @Test
  void structuredOutputIsAdvertisedAsSupported() {
    ChatModel model = provider.setup(new ModelOptions(MODEL, true, List.of()));
    assertThat(model.supportedCapabilities())
        .contains(Capability.RESPONSE_FORMAT_JSON_SCHEMA);
  }

  @Test
  void supportsEmbedding() {
    assertThat(provider.supportsEmbedding()).isTrue();
  }

  @Test
  void secretsVarsIsEmpty() {
    // Ollama has no API key
    assertThat(provider.secretsVars()).isEmpty();
  }

  private static ChatModelProvider loadProvider() {
    return ChatModelFactory.create(OllamaModelProvider.NAME).get();
  }
}
