package com.axonivy.utils.smart.workflow.model.aura;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.axonivy.utils.smart.workflow.model.ChatModelFactory;
import com.axonivy.utils.smart.workflow.model.aura.internal.AuraChatModel;
import com.axonivy.utils.smart.workflow.model.aura.internal.AuraServiceConnector.AuraConf;
import com.axonivy.utils.smart.workflow.model.spi.ChatModelProvider;
import com.axonivy.utils.smart.workflow.model.spi.ModelOptions;

import ch.ivyteam.ivy.environment.AppFixture;
import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
class TestAuraLoader {

  private ChatModelProvider provider;

  @BeforeEach
  void setup(AppFixture fixture) {
    fixture.var(AuraConf.ORG_ID, "test-org");
    fixture.var(AuraConf.HOST, "aura.example.invalid");
    fixture.var(AuraConf.APP_ID, "test-app");
    provider = ChatModelFactory.create(AuraModelProvider.NAME).orElseThrow();
  }

  @Test
  void loadWithoutConfiguration(AppFixture fixture) {
    fixture.var(AuraConf.ORG_ID, "");
    assertThat(ChatModelFactory.create("Aura")).isPresent();
    assertThat(provider.models()).isEmpty();
  }

  @Test
  void usesAuraChatModel() {
    assertThat(provider.setup(ModelOptions.builder().build())).isInstanceOf(AuraChatModel.class);
  }

  @Test
  void modelSelectionIsRejected() {
    assertThatThrownBy(() -> provider.setup(ModelOptions.builder().modelName("some-model").build()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("does not support model selection");
  }

  @Test
  void customParameters() {
    var model = provider.setup(ModelOptions.builder().temperature(0.7).maxOutputTokens(512).build());
    assertThat(model.defaultRequestParameters().temperature()).isEqualTo(0.7);
    assertThat(model.defaultRequestParameters().maxOutputTokens()).isEqualTo(512);
  }

  @Test
  void structuredOutputIsRejected() {
    assertThatThrownBy(() -> provider.setup(ModelOptions.builder().structuredOutput(true).build()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("structured output is not implemented");
  }

  @Test
  void toolsAreRejected() {
    assertThatThrownBy(() -> provider.setup(ModelOptions.builder().hasTools(true).build()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("tool calls are not implemented");
  }

  @Test
  void secretsVarsIsEmpty() {
    assertThat(provider.secretsVars()).isEmpty();
    assertThat(provider.supportsEmbedding()).isFalse();
  }

  @Test
  void missingOrgId(AppFixture fixture) {
    fixture.var(AuraConf.ORG_ID, "");
    assertThatThrownBy(() -> provider.setup(ModelOptions.builder().build()))
        .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(AuraConf.ORG_ID);
  }

  @Test
  void missingHost(AppFixture fixture) {
    fixture.var(AuraConf.HOST, "");
    assertThatThrownBy(() -> provider.setup(ModelOptions.builder().build()))
        .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(AuraConf.HOST);
  }

  @Test
  void missingAppId(AppFixture fixture) {
    fixture.var(AuraConf.APP_ID, "");
    assertThatThrownBy(() -> provider.setup(ModelOptions.builder().build()))
        .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(AuraConf.APP_ID);
  }
}
