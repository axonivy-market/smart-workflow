package com.axonivy.utils.smart.workflow.model.aura;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.axonivy.utils.smart.workflow.model.ChatModelFactory;
import com.axonivy.utils.smart.workflow.model.ChatModelFactory.AiConf;
import com.axonivy.utils.smart.workflow.model.EmbeddingModelFactory;
import com.axonivy.utils.smart.workflow.model.aura.internal.AuraServiceConnector.AuraConf;
import com.axonivy.utils.smart.workflow.model.spi.ChatModelProvider;
import com.axonivy.utils.smart.workflow.model.spi.ModelOptions;

import ch.ivyteam.ivy.environment.AppFixture;
import ch.ivyteam.ivy.environment.IvyTest;
import dev.langchain4j.model.chat.Capability;

@IvyTest
class TestAuraLoader {

  private ChatModelProvider provider;

  @BeforeEach
  void setup(AppFixture fixture) {
    fixture.var(AuraConf.BASE_URL, "https://aura.example.invalid/docuware/aura");
    fixture.var(AuraConf.ORGANIZATION_ID, "550e8400-e29b-41d4-a716-446655440000");
    fixture.var(AuraConf.BEARER_TOKEN, "synthetic-test-token");
    provider = ChatModelFactory.create(AuraModelProvider.NAME).orElseThrow();
  }

  @Test
  void discoveredForProviderSelectorWithoutCredentials(AppFixture fixture) {
    fixture.var(AuraConf.BASE_URL, "");
    fixture.var(AuraConf.BEARER_TOKEN, "");
    assertThat(ChatModelFactory.providers()).extracting(ChatModelProvider::name).contains(AuraModelProvider.NAME);
  }

  @Test
  void globalAndExplicitSelection(AppFixture fixture) {
    fixture.var(AiConf.DEFAULT_PROVIDER, AuraModelProvider.NAME);
    assertThat(ChatModelFactory.getProviderOrDefault(null).name()).isEqualTo(AuraModelProvider.NAME);
    fixture.var(AiConf.DEFAULT_PROVIDER, "OpenAI");
    assertThat(ChatModelFactory.getProviderOrDefault(AuraModelProvider.NAME).name()).isEqualTo(AuraModelProvider.NAME);
  }

  @Test
  void noInventedModelOrEmbeddingSupport() {
    assertThat(provider.models()).isEmpty();
    assertThat(provider.supportsEmbedding()).isFalse();
    assertThat(EmbeddingModelFactory.providers()).extracting(ChatModelProvider::name).doesNotContain(AuraModelProvider.NAME);
    var model = provider.setup(ModelOptions.builder().build());
    assertThat(model.defaultRequestParameters().modelName()).isNull();
  }

  @Test
  void capabilitiesAndRequestOptions() {
    var model = provider.setup(ModelOptions.builder().structuredOutput(true).temperature(0.4).maxOutputTokens(512).build());
    assertThat(model.supportedCapabilities()).contains(Capability.RESPONSE_FORMAT_JSON_SCHEMA);
    assertThat(model.defaultRequestParameters().temperature()).isEqualTo(0.4);
    assertThat(model.defaultRequestParameters().maxOutputTokens()).isEqualTo(512);
  }

  @Test
  void registersBearerTokenAsSecret() {
    assertThat(provider.secretsVars()).containsExactly(AuraConf.BEARER_TOKEN);
  }

  @Test
  void rejectsExplicitModelAndTools() {
    assertThatThrownBy(() -> provider.setup(ModelOptions.builder().modelName("AwesomeModel").build()))
        .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("leave Model empty");
    assertThatThrownBy(() -> provider.setup(ModelOptions.builder().hasTools(true).build()))
        .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Available tools empty");
  }

  @ParameterizedTest
  @ValueSource(strings = {AuraConf.BASE_URL, AuraConf.ORGANIZATION_ID, AuraConf.BEARER_TOKEN})
  void missingConfigurationFailsExplicitly(String variable, AppFixture fixture) {
    fixture.var(variable, "");
    assertThatThrownBy(() -> provider.setup(ModelOptions.builder().build()))
        .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Aura").hasNoCause();
  }

  @ParameterizedTest
  @ValueSource(strings = {"relative", "http://aura.example.invalid", "ftp://aura.example.invalid",
      "https://user:do-not-echo@aura.example.invalid", "https://aura.example.invalid?credential=do-not-echo",
      "https://aura.example.invalid#fragment", "https://[do-not-echo"})
  void invalidEndpointsAreRejectedWithoutEcho(String endpoint, AppFixture fixture) {
    fixture.var(AuraConf.BASE_URL, endpoint);
    assertThatThrownBy(() -> provider.setup(ModelOptions.builder().build()))
        .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("BaseUrl")
        .hasMessageNotContaining("do-not-echo").hasNoCause();
  }

  @ParameterizedTest
  @ValueSource(strings = {"not-an-org-do-not-echo", "../../other-org"})
  void invalidOrganizationIsRejectedWithoutEcho(String organization, AppFixture fixture) {
    fixture.var(AuraConf.ORGANIZATION_ID, organization);
    assertThatThrownBy(() -> provider.setup(ModelOptions.builder().build()))
        .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("OrganizationId")
        .hasMessageNotContaining("do-not-echo").hasNoCause();
  }

  @ParameterizedTest
  @ValueSource(strings = {"0", "-1", "not-a-timeout", "999999999999999999999"})
  void invalidTimeoutFailsExplicitly(String timeout, AppFixture fixture) {
    fixture.var(AuraConf.TIMEOUT_SECONDS, timeout);
    assertThatThrownBy(() -> provider.setup(ModelOptions.builder().build()))
        .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("TimeoutSeconds").hasNoCause();
  }

  @ParameterizedTest
  @ValueSource(strings = {"Bearer not-a-token", "token\r\nAuthorization: do-not-echo"})
  void malformedBearerTokenIsRejectedWithoutEcho(String token, AppFixture fixture) {
    fixture.var(AuraConf.BEARER_TOKEN, token);
    assertThatThrownBy(() -> provider.setup(ModelOptions.builder().build()))
        .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("BearerToken")
        .hasMessageNotContaining("do-not-echo").hasNoCause();
  }
}
