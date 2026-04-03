package com.axonivy.utils.smart.workflow.guardrails;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.Response;

import com.axonivy.utils.ai.mock.MockOpenAI;
import com.axonivy.utils.smart.workflow.client.OpenAiTestClient;
import com.axonivy.utils.smart.workflow.guardrails.provider.DefaultGuardrailProvider;
import com.axonivy.utils.smart.workflow.model.openai.internal.OpenAiServiceConnector.OpenAiConf;

import Features.GuardrailDemoData;
import ch.ivyteam.ivy.bpm.engine.client.BpmClient;
import ch.ivyteam.ivy.bpm.engine.client.element.BpmProcess;
import ch.ivyteam.ivy.environment.AppFixture;
import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.test.RestResourceTest;

@RestResourceTest
public class TestOutputGuardrailProcess {
  private static final BpmProcess GUARDRAIL_DEMO = BpmProcess.name("GuardrailDemo");

  @BeforeEach
  void setup(AppFixture fixture) {
    fixture.var(OpenAiConf.BASE_URL, OpenAiTestClient.localMockApiUrl("guardrails"));
    fixture.var(OpenAiConf.API_KEY, "");
    fixture.var(DefaultGuardrailProvider.DEFAULT_OUTPUT_GUARDRAILS, "");
    MockOpenAI.defineChat(_ -> buildResponse());
  }

  private static Response buildResponse() {
    /*
    * The following fake credentials are used for testing purposes only and do not provide
    * access to any production systems. Please do not submit them as part of a bug bounty program.
    * ResourceResponder#send() is not used here because the test requires string concatenation
    * to prevent false positives from automated sensitive-data detection tools.
    */
    String apiKeyExample = "sk-" + "1234567890abcdef1234567890abcdef";
    String body = """
        {
          "id": "chatcmpl-test",
          "object": "chat.completion",
          "created": 1773582770,
          "model": "gpt-4.1-mini",
          "choices": [{
            "index": 0,
            "message": {"role": "assistant", "content": "Example API key: %s"},
            "finish_reason": "stop"
          }],
          "usage": {"prompt_tokens": 10, "completion_tokens": 10, "total_tokens": 20}
        }
        """.formatted(apiKeyExample);
    return Response.ok().entity(body).build();
  }

  @Test
  void testCatchSensitiveDataOutput(BpmClient client) {
    Ivy.session().loginSessionUser("James", "secret");
    var res = client.start()
        .process(GUARDRAIL_DEMO.elementName("Sensitive Data Output Guardrail Demo"))
        .as().session(Ivy.session())
        .execute();
    GuardrailDemoData data = res.data().last();
    assertThat(data.getResult()).contains("sensitive data");
  }
}
