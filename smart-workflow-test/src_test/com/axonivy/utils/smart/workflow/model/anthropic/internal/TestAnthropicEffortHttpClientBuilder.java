package com.axonivy.utils.smart.workflow.model.anthropic.internal;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.axonivy.utils.smart.workflow.utils.JsonUtils;

class TestAnthropicEffortHttpClientBuilder {

  @Test
  void addsEffortWithoutDiscardingStructuredOutputConfig() {
    String requestBody = AnthropicEffortHttpClientBuilder.withEffort(
        "{\"output_config\":{\"format\":{\"type\":\"json_schema\"}}}", "medium");

    var request = JsonUtils.getObjectMapper().readTree(requestBody);
    assertThat(request.at("/output_config/effort").asString()).isEqualTo("medium");
    assertThat(request.at("/output_config/format/type").asString()).isEqualTo("json_schema");
  }

  @Test
  void createsOutputConfigWhenNotProvided() {
    String requestBody = AnthropicEffortHttpClientBuilder.withEffort("{\"model\":\"claude\"}", "high");

    var request = JsonUtils.getObjectMapper().readTree(requestBody);
    assertThat(request.at("/output_config/effort").asString()).isEqualTo("high");
  }
}
