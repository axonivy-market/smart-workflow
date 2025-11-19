package com.axonivy.utils.smart.workflow.model.openai;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.axonivy.utils.smart.workflow.model.openai.internal.OpenAiServiceConnector;
import com.axonivy.utils.smart.workflow.model.openai.internal.OpenAiServiceConnector.OpenAiConf;

import ch.ivyteam.ivy.environment.AppFixture;
import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
public class TestOpenAiModelConfig {

  @Test
  void defaultsModel_fromIvyVariable(AppFixture fixture) {
    String legacy35 = "gpt-3.5-turbo";
    fixture.var(OpenAiConf.DEFAULT_MODEL, legacy35);
    var builder = OpenAiServiceConnector.buildOpenAiModel(legacy35);
    assertThat(builder).hasFieldOrPropertyWithValue("modelName", legacy35);
  }

  @Test
  void defaultsModel(AppFixture fixture) {
    fixture.var(OpenAiConf.DEFAULT_MODEL, "");
    var builder = OpenAiServiceConnector.buildOpenAiModel();
    assertThat(builder).hasFieldOrPropertyWithValue("modelName", "gpt-4.1-mini");
  }

}
