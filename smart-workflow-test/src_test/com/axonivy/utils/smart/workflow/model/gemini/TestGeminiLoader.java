package com.axonivy.utils.smart.workflow.model.gemini;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.axonivy.utils.smart.workflow.model.ChatModelFactory;
import com.axonivy.utils.smart.workflow.model.gemini.internal.GeminiServiceConnector.GeminiConf;
import com.axonivy.utils.smart.workflow.model.gemini.internal.enums.GoogleAiGeminiChatModelName;
import com.axonivy.utils.smart.workflow.model.spi.ChatModelProvider;
import com.axonivy.utils.smart.workflow.model.spi.ModelOptions;

import ch.ivyteam.ivy.environment.AppFixture;
import ch.ivyteam.ivy.environment.IvyTest;
import dev.langchain4j.model.chat.ChatModel;
@IvyTest
public class TestGeminiLoader {

  private static final String MODEL = GoogleAiGeminiChatModelName.GEMINI_1_5_FLASH.toString();
  private static final String API_KEY = "${decrypt:test-key-1}";

  private ChatModelProvider provider;

  @BeforeEach
  void setup(AppFixture fixture) {
    fixture.var(GeminiConf.DEFAULT_MODEL, MODEL);
    fixture.var(GeminiConf.API_KEY, API_KEY);
    provider = loadModel();
  }

  @Test
  void load() {
    assertThat(provider).isNotNull();
    assertThat(provider.name()).isEqualTo(GeminiModelProvider.NAME);
  }

  @Test
  void resolveByName() {
    assertThat(ChatModelFactory.create(GeminiModelProvider.NAME)).isPresent();
  }

  @Test
  void resolveBase() {
    assertThat(ChatModelFactory.create(GeminiModelProvider.NAME)).isNotEmpty();
  }

  @Test
  void listModels() {
    assertThat(provider.models()).contains(
        GoogleAiGeminiChatModelName.GEMINI_1_5_FLASH.toString(),
        GoogleAiGeminiChatModelName.GEMINI_1_5_PRO.toString()
    );
  }

  @Test
  void capabilities() {
    ModelOptions options = ModelOptions.builder()
        .modelName(MODEL)
        .build();
    ChatModel normal = provider.setup(options);
    assertThat(normal.supportedCapabilities()).isEmpty();

    ChatModel structured = provider.setup(ModelOptions.builder()
        .modelName(MODEL)
        .structuredOutput(true)
        .build());
    assertThat(structured.supportedCapabilities()).isEmpty();
  }

  @Test
  void customTemperature() {
    ChatModel customTemp = provider.setup(ModelOptions.builder()
        .modelName(MODEL)
        .temperature(0.7)
        .build());
    assertThat(customTemp.defaultRequestParameters().temperature()).isEqualTo(0.7);
  }

  private static ChatModelProvider loadModel() {
    return ChatModelFactory.create(GeminiModelProvider.NAME).get();
  }
}
