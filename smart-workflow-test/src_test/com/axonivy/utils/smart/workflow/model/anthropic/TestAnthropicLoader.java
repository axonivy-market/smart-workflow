package com.axonivy.utils.smart.workflow.model.anthropic;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.axonivy.utils.smart.workflow.model.ChatModelFactory;
import com.axonivy.utils.smart.workflow.model.anthropic.internal.AnthropicServiceConnector.AnthropicConf;
import com.axonivy.utils.smart.workflow.model.spi.ChatModelProvider;
import com.axonivy.utils.smart.workflow.model.spi.ChatModelProvider.ModelOptions;

import ch.ivyteam.ivy.environment.AppFixture;
import ch.ivyteam.ivy.environment.IvyTest;
import dev.langchain4j.model.anthropic.AnthropicChatModelName;
import dev.langchain4j.model.chat.Capability;
import dev.langchain4j.model.chat.ChatModel;

@IvyTest
public class TestAnthropicLoader {

  private static final String MODEL = AnthropicChatModelName.CLAUDE_HAIKU_4_5_20251001.toString();
  private static final String API_KEY = "${decrypt:test-key-1}";
  
  private ChatModelProvider provider;

  @BeforeEach
  void setup(AppFixture fixture) {
    fixture.var(AnthropicConf.DEFAULT_MODEL, MODEL);
    fixture.var(AnthropicConf.API_KEY, API_KEY);
    provider = loadModel();
  }

  @Test
  void load() {
    assertThat(provider).isNotNull();
    assertThat(provider.name()).isEqualTo(AnthropicModelProvider.NAME);
  }

  @Test
  void listModels() {
    assertThat(provider.models()).contains(
      "claude-opus-4-6",
      "claude-sonnet-4-6",
      "claude-haiku-4-5-20251001"
    );
  }

  @Test
  void capabilities() {
    ChatModel structured = provider.setup(new ModelOptions(MODEL, true, List.of()));
    assertThat(structured.supportedCapabilities()).contains(Capability.RESPONSE_FORMAT_JSON_SCHEMA);
  }

  private static ChatModelProvider loadModel() {
    return ChatModelFactory.create(AnthropicModelProvider.NAME).get();
  }
}
