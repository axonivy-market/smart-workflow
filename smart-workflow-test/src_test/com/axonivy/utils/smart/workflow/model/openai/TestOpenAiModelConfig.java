package com.axonivy.utils.smart.workflow.model.openai;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.axonivy.utils.smart.workflow.model.openai.internal.OpenAiServiceConnector;
import com.axonivy.utils.smart.workflow.model.openai.internal.OpenAiServiceConnector.OpenAiConf;

import ch.ivyteam.ivy.environment.AppFixture;
import ch.ivyteam.ivy.environment.IvyTest;
import dev.langchain4j.model.chat.request.DefaultChatRequestParameters;
import dev.langchain4j.model.openai.OpenAiChatModel.OpenAiChatModelBuilder;

@IvyTest
public class TestOpenAiModelConfig {

  @Test
  void defaultsModel_fromIvyVariable(AppFixture fixture) {
    String legacy35 = "gpt-3.5-turbo";
    fixture.var(OpenAiConf.DEFAULT_MODEL, legacy35);
    var builder = OpenAiServiceConnector.buildOpenAiModel(legacy35);
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
    var builder = OpenAiServiceConnector.buildOpenAiModel("gpt-4.1-mini");
    assertThat(builder).extracting("defaultRequestParameters")
        .extracting(r -> ((DefaultChatRequestParameters) r).temperature())
        .isEqualTo(0.0);
  }

  @Test
  void temperature_gpt5(AppFixture fixture) {
    var builder = OpenAiServiceConnector.buildOpenAiModel("gpt-5");
    assertThat(builder).extracting("defaultRequestParameters")
        .extracting(r -> ((DefaultChatRequestParameters) r).temperature())
        .isEqualTo(1.0);
  }

  @Test
  void temperature_gpt5_nano(AppFixture fixture) {
    var builder = OpenAiServiceConnector.buildOpenAiModel("gpt-5-nano");
    assertThat(builder).extracting("defaultRequestParameters")
        .extracting(r -> ((DefaultChatRequestParameters) r).temperature())
        .isEqualTo(1.0);
  }

  private static void assertModel(OpenAiChatModelBuilder builder, String expect) {
    assertThat(builder).extracting("defaultRequestParameters")
      .extracting(r -> ((DefaultChatRequestParameters)r).modelName())
      .isEqualTo(expect);
  }

}
