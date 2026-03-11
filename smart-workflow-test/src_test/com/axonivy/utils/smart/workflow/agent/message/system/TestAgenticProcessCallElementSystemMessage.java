package com.axonivy.utils.smart.workflow.agent.message.system;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.axonivy.utils.ai.mock.MockOpenAI;
import com.axonivy.utils.smart.workflow.client.OpenAiTestClient;
import com.axonivy.utils.smart.workflow.model.openai.internal.OpenAiServiceConnector.OpenAiConf;
import com.axonivy.utils.smart.workflow.test.TestToolUserData;

import ch.ivyteam.ivy.bpm.engine.client.BpmClient;
import ch.ivyteam.ivy.bpm.engine.client.element.BpmProcess;
import ch.ivyteam.ivy.environment.AppFixture;
import ch.ivyteam.test.RestResourceTest;
import ch.ivyteam.test.log.LoggerAccess;
import ch.ivyteam.test.resource.ResourceResponder;
import dev.langchain4j.http.client.log.LoggingHttpClient;

@RestResourceTest
class TestAgenticProcessCallElementSystemMessage {

  private static final BpmProcess AGENT_TOOLS = BpmProcess.name("TestToolUser");

  @RegisterExtension
  LoggerAccess log = new LoggerAccess(LoggingHttpClient.class.getName());

  @BeforeEach
  void setup(AppFixture fixture, ResourceResponder responder) {
    fixture.var(OpenAiConf.BASE_URL, OpenAiTestClient.localMockApiUrl("systemMessage"));
    fixture.var(OpenAiConf.API_KEY, "");
    MockOpenAI.defineChat(request -> responder.send("response.json"));
  }

  @Test
  void systemMessage(BpmClient client) {
    var res = client.start().process(AGENT_TOOLS.elementName("systemMessage")).execute();
    TestToolUserData data = res.data().last();
    assertThat(data.getResult())
        .contains("it's so hot, right?");
  }

}
