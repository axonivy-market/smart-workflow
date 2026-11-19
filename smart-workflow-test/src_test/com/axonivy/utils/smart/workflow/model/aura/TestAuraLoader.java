package com.axonivy.utils.smart.workflow.model.aura;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.axonivy.utils.smart.workflow.model.ChatModelFactory;
import com.axonivy.utils.smart.workflow.model.aura.internal.AuraServiceConnector.AuraConf;
import com.axonivy.utils.smart.workflow.model.spi.ChatModelProvider;
import com.axonivy.utils.smart.workflow.model.spi.ModelOptions;

import ch.ivyteam.ivy.environment.AppFixture;
import ch.ivyteam.ivy.environment.IvyTest;
import dev.langchain4j.model.chat.Capability;
import dev.langchain4j.model.openai.OpenAiChatModel;

@IvyTest
class TestAuraLoader {

  private ChatModelProvider provider;

  @BeforeEach
  void setup(AppFixture fixture) {
    fixture.var(AuraConf.BASE_URL, "https://aura.example.invalid/v1/");
    fixture.var(AuraConf.DEFAULT_MODEL, "test-aura-model");
    fixture.var(AuraConf.API_KEY, "");
    provider = ChatModelFactory.create(AuraModelProvider.NAME).orElseThrow();
  }

  @Test
  void loadWithoutConfiguration(AppFixture fixture) {
    fixture.var(AuraConf.BASE_URL, "");
    fixture.var(AuraConf.DEFAULT_MODEL, "");
    assertThat(ChatModelFactory.create("Aura")).isPresent();
    assertThat(provider.models()).isEmpty();
  }

  @Test
  void usesOpenAiChatModel() {
    assertThat(provider.setup(ModelOptions.builder().build())).isInstanceOf(OpenAiChatModel.class);
  }

  @Test
  void defaultModel() {
    assertThat(provider.setup(ModelOptions.builder().build()).defaultRequestParameters().modelName())
        .isEqualTo("test-aura-model");
    assertThat(provider.models()).containsExactly("test-aura-model");
  }

  @Test
  void explicitModelOverridesDefault() {
    var model = provider.setup(ModelOptions.builder().modelName("another-model").build());
    assertThat(model.defaultRequestParameters().modelName()).isEqualTo("another-model");
  }

  @Test
  void explicitModelWithoutDefault(AppFixture fixture) {
    fixture.var(AuraConf.DEFAULT_MODEL, "");
    var model = provider.setup(ModelOptions.builder().modelName("explicit-model").build());
    assertThat(model.defaultRequestParameters().modelName()).isEqualTo("explicit-model");
  }

  @Test
  void structuredOutput() {
    var model = provider.setup(ModelOptions.builder().structuredOutput(true).build());
    assertThat(model.supportedCapabilities()).contains(Capability.RESPONSE_FORMAT_JSON_SCHEMA);
  }

  @Test
  void customParameters() {
    var model = provider.setup(ModelOptions.builder().temperature(0.7).maxOutputTokens(512).build());
    assertThat(model.defaultRequestParameters().temperature()).isEqualTo(0.7);
    assertThat(model.defaultRequestParameters().maxOutputTokens()).isEqualTo(512);
  }

  @Test
  void secretVariable() {
    assertThat(provider.secretsVars()).containsExactly(AuraConf.API_KEY);
    assertThat(provider.supportsEmbedding()).isFalse();
  }

  @Test
  void missingBaseUrl(AppFixture fixture) {
    fixture.var(AuraConf.BASE_URL, "");
    assertThatThrownBy(() -> provider.setup(ModelOptions.builder().build()))
        .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(AuraConf.BASE_URL);
  }

  @Test
  void missingModel(AppFixture fixture) {
    fixture.var(AuraConf.DEFAULT_MODEL, "");
    assertThatThrownBy(() -> provider.setup(ModelOptions.builder().build()))
        .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(AuraConf.DEFAULT_MODEL);
  }
}
