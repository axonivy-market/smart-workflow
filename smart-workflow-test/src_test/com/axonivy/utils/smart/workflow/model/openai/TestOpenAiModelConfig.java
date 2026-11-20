package com.axonivy.utils.smart.workflow.model.openai;

import static org.assertj.core.api.Assertions.assertThat;


import org.junit.jupiter.api.Test;

import com.axonivy.utils.smart.workflow.model.openai.internal.OpenAiServiceConnector;
import com.axonivy.utils.smart.workflow.model.openai.internal.OpenAiServiceConnector.OpenAiConf;
import com.axonivy.utils.smart.workflow.model.spi.ModelOptions;

import ch.ivyteam.ivy.environment.AppFixture;
import ch.ivyteam.ivy.environment.IvyTest;
import dev.langchain4j.model.chat.request.DefaultChatRequestParameters;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel.OpenAiChatModelBuilder;
import dev.langchain4j.model.openai.OpenAiChatRequestParameters;
import dev.langchain4j.model.openai.OpenAiResponsesChatModel;
import dev.langchain4j.model.openai.OpenAiResponsesChatRequestParameters;

@IvyTest
public class TestOpenAiModelConfig {

  @Test
  void defaultsModel_fromIvyVariable(AppFixture fixture) {
    String legacy35 = "gpt-3.5-turbo";
    fixture.var(OpenAiConf.DEFAULT_MODEL, legacy35);
    var builder = OpenAiServiceConnector.buildOpenAiModel(buildModelOptions(legacy35, null));
    assertModel(builder, legacy35);
  }

  @Test
  void defaultsModel(AppFixture fixture) {
    fixture.var(OpenAiConf.DEFAULT_MODEL, "");
    var builder = OpenAiServiceConnector.buildOpenAiModel();
    assertModel(builder, "gpt-4.1-mini");
  }

  @Test
  void temperature_gpt4(AppFixture fixture) {
    var builder = OpenAiServiceConnector.buildOpenAiModel(buildModelOptions("gpt-4.1-mini", null));
    assertThat(builder).extracting("defaultRequestParameters")
        .extracting(r -> ((DefaultChatRequestParameters) r).temperature())
        .isEqualTo(0.0);
  }

  @Test
  void temperature_gpt5(AppFixture fixture) {
    var builder = OpenAiServiceConnector.buildOpenAiModel(buildModelOptions("gpt-5", null));
    assertThat(builder).extracting("defaultRequestParameters")
        .extracting(r -> ((DefaultChatRequestParameters) r).temperature())
        .isEqualTo(1.0);
  }

  @Test
  void temperature_gpt5_nano(AppFixture fixture) {
    var builder = OpenAiServiceConnector.buildOpenAiModel(buildModelOptions("gpt-5-nano", null));
    assertThat(builder).extracting("defaultRequestParameters")
        .extracting(r -> ((DefaultChatRequestParameters) r).temperature())
        .isEqualTo(1.0);
  }

  @Test
  void temperature_custom(AppFixture fixture) {
    var builder = OpenAiServiceConnector.buildOpenAiModel(buildModelOptions("gpt-4.1-mini", 0.5));
    assertThat(builder).extracting("defaultRequestParameters")
        .extracting(r -> ((DefaultChatRequestParameters) r).temperature())
        .isEqualTo(0.5);
  }

  @Test
  void reasoningEffort_custom(AppFixture fixture) {
    var options = ModelOptions.builder()
        .modelName("gpt-6-astra")
        .reasoningEffort("max")
        .build();
    var model = new OpenAiModelProvider().setup(options);
    assertThat(model).isInstanceOf(OpenAiResponsesChatModel.class);
    assertThat(model.defaultRequestParameters())
        .extracting(r -> ((OpenAiResponsesChatRequestParameters) r).reasoningEffort())
        .isEqualTo("max");
  }

  @Test
  void reasoningEffort_unsupportedModel_isNotSent(AppFixture fixture) {
    var options = ModelOptions.builder()
        .modelName("gpt-4.1-mini")
        .reasoningEffort("max")
        .build();
    var model = new OpenAiModelProvider().setup(options);

    assertThat(model).isInstanceOf(OpenAiChatModel.class);
    assertThat(model.defaultRequestParameters())
        .extracting(r -> ((OpenAiChatRequestParameters) r).reasoningEffort())
        .isNull();
  }

  @Test
  void reasoningEffort_supportedModelNames() {
    assertThat(OpenAiServiceConnector.supportsReasoningEffort("o3-mini")).isTrue();
    assertThat(OpenAiServiceConnector.supportsReasoningEffort("gpt-5.5")).isTrue();
    assertThat(OpenAiServiceConnector.supportsReasoningEffort("gpt-6.2-astra")).isTrue();
    assertThat(OpenAiServiceConnector.supportsReasoningEffort("gpt-4.1-mini")).isFalse();
    assertThat(OpenAiServiceConnector.supportsReasoningEffort("gpt-4o")).isFalse();
  }

  @Test
  void reasoningEffort_usesConfiguredDefaultModel(AppFixture fixture) {
    fixture.var(OpenAiConf.DEFAULT_MODEL, "gpt-5.5");

    assertThat(OpenAiServiceConnector.supportsReasoningEffort(null)).isTrue();
  }

  @Test
  void withoutReasoningEffort_usesChatCompletions(AppFixture fixture) {
    var model = new OpenAiModelProvider().setup(ModelOptions.builder()
        .modelName("gpt-4.1-mini")
        .build());
    assertThat(model).isInstanceOf(OpenAiChatModel.class);
  }

  private static void assertModel(OpenAiChatModelBuilder builder, String expect) {
    assertThat(builder).extracting("defaultRequestParameters")
      .extracting(r -> ((DefaultChatRequestParameters)r).modelName())
      .isEqualTo(expect);
  }

  private static ModelOptions buildModelOptions(String modelName, Double temperature) {
    return ModelOptions.builder()
        .modelName(modelName)
        .temperature(temperature)
        .build();
  }
}
