package com.axonivy.utils.smart.workflow.model.observer;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.axonivy.utils.ai.mock.MockOpenAI;
import com.axonivy.utils.smart.workflow.client.OpenAiTestClient;
import com.axonivy.utils.smart.workflow.demo.support.mock.SupportToolChat;
import com.axonivy.utils.smart.workflow.model.openai.internal.OpenAiServiceConnector.OpenAiConf;

import AgentDemo.SupportAgentToolsData;
import ch.ivyteam.ivy.bpm.engine.client.BpmClient;
import ch.ivyteam.ivy.bpm.engine.client.element.BpmElement;
import ch.ivyteam.ivy.bpm.engine.client.element.BpmProcess;
import ch.ivyteam.ivy.environment.AppFixture;
import ch.ivyteam.ivy.trace.Attribute;
import ch.ivyteam.ivy.trace.Tracer;
import ch.ivyteam.test.RestResourceTest;

@RestResourceTest
class TestObserverSpan {

  private static final BpmProcess AGENT_TOOLS = BpmProcess.name("SupportAgentTools");
  private Tracer tracer;

  @BeforeEach
  void setup(AppFixture fixture) {
    fixture.var(OpenAiConf.BASE_URL, OpenAiTestClient.localMockApiUrl("tool"));
    fixture.var(OpenAiConf.API_KEY, "");
    MockOpenAI.defineChat(new SupportToolChat()::toolTest);

    this.tracer = Tracer.instance();
    tracer.start();
  }

  @AfterEach
  void tearDown() {
    tracer.stop();
  }

  @Test
  void observesModelInteractions(BpmClient client) {
    var res = client.start().process(AGENT_TOOLS).execute();
    var ticketDone = (SupportAgentToolsData) res.data().onElement(BpmElement.pid("19856884121ED111-f1"))
        .getLast();
    assertThat(ticketDone.getSupportTicket().getType().name()).isEqualToIgnoringCase("technical");

    var spans =  tracer.slowTraces().all();
    var children = spans.get(0).rootSpan().children();
    var chat = children.stream().filter(t -> t.name().equals("AI Assistant")  ).findFirst();
    var attrs = chat.get().attributes().stream().collect(Collectors.toMap(Attribute::name, Attribute::value));
    assertThat(attrs.keySet())
      .contains("gen_ai.request.model");
  }

}
