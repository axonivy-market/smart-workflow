package com.axonivy.utils.smart.workflow.model.aura.internal;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.axonivy.utils.ai.mock.MockAura;
import com.axonivy.utils.smart.workflow.model.spi.ModelOptions;

import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.test.RestResourceTest;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.request.ChatRequest;
import jakarta.ws.rs.core.Response;

@RestResourceTest
class TestAuraServiceConnector {

  @Test
  void postsAuraInferenceRequestAndReturnsTopLevelContent() {
    MockAura.defineInference(request -> {
      assertThat(request.get("messages").get(0).get("role").asString()).isEqualTo("user");
      assertThat(request.get("messages").get(0).get("content").asString()).isEqualTo("hello test");
      assertThat(request.get("stream").asBoolean()).isFalse();
      assertThat(request.get("use_history").asBoolean()).isFalse();
      assertThat(request.get("temperature").asDouble()).isEqualTo(0.0);
      return Response.ok("{\"content\":\"Aura response\"}").type("application/json").build();
    });

    var target = Ivy.rest().client("auraMockClient")
        .path("api/v1/test-org/inference");
    var model = AuraServiceConnector.buildAuraModel(ModelOptions.builder().build(), target);

    var response = model.chat(ChatRequest.builder().messages(UserMessage.from("hello test")).build());

    assertThat(response.aiMessage().text()).isEqualTo("Aura response");
  }
}
