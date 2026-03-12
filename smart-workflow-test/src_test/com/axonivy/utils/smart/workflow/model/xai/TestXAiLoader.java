package com.axonivy.utils.smart.workflow.model.xai;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.axonivy.utils.smart.workflow.model.ChatModelFactory;
import com.axonivy.utils.smart.workflow.model.spi.ChatModelProvider;
import com.axonivy.utils.smart.workflow.model.spi.ChatModelProvider.ModelOptions;
import com.axonivy.utils.smart.workflow.model.xai.internal.XAiServiceConnector;

import ch.ivyteam.ivy.environment.AppFixture;
import ch.ivyteam.ivy.environment.IvyTest;
import dev.langchain4j.model.chat.Capability;
import dev.langchain4j.model.chat.ChatModel;

@IvyTest
public class TestXAiLoader {

  private static final String MODEL = "grok-4-1-fast";
  private static final String API_KEY = "${decrypt:test-key-1}";

  private ChatModelProvider provider;

  @BeforeEach
  void setup(AppFixture fixture) {
    fixture.var(XAiServiceConnector.XAiConf.DEFAULT_MODEL, MODEL);
    fixture.var(XAiServiceConnector.XAiConf.API_KEY, API_KEY);
    provider = loadModel();
  }

  @Test
  void load() {
    assertThat(provider).isNotNull();
    assertThat(provider.name()).isEqualTo(XAiModelProvider.NAME);
  }

  @Test
  void resolveByName() {
    assertThat(ChatModelFactory.create(XAiModelProvider.NAME)).isPresent();
  }

  @Test
  void listModels() {
    assertThat(provider.models()).contains(
        "grok-4-1-fast",
        "grok-4-1-mini"
    );
  }

  @Test
  void capabilities() {
    ChatModel structured = provider.setup(new ModelOptions(MODEL, true), List.of());
    assertThat(structured.supportedCapabilities()).contains(Capability.RESPONSE_FORMAT_JSON_SCHEMA);
  }

  private static ChatModelProvider loadModel() {
    return ChatModelFactory.create(XAiModelProvider.NAME).get();
  }
}
