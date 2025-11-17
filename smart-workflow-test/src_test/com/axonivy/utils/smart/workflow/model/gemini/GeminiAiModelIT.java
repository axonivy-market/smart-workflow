package com.axonivy.utils.smart.workflow.model.gemini;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.axonivy.utils.smart.workflow.model.ChatModelFactory.AiConf;
import com.axonivy.utils.smart.workflow.model.gemini.internal.GeminiServiceConnector;
import com.axonivy.utils.smart.workflow.test.TestToolUserData;
import com.axonivy.utils.smart.workflow.test.utils.TestUtils;

import ch.ivyteam.ivy.bpm.engine.client.BpmClient;
import ch.ivyteam.ivy.bpm.engine.client.element.BpmProcess;
import ch.ivyteam.ivy.bpm.exec.client.IvyProcessTest;
import ch.ivyteam.ivy.environment.AppFixture;
import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.test.log.LoggerAccess;
import dev.langchain4j.http.client.log.LoggingHttpClient;

@IvyProcessTest
public class GeminiAiModelIT {

  private static final BpmProcess AGENT_TOOLS = BpmProcess.name("TestToolUser");

  @RegisterExtension
  LoggerAccess log = new LoggerAccess(LoggingHttpClient.class.getName());

  @BeforeEach
  void setup(AppFixture fixture) {
    fixture.var(AiConf.DEFAULT_PROVIDER, GeminiModelProvider.NAME);
    fixture.var(GeminiServiceConnector.GeminiConf.API_KEY, TestUtils.getSystemProperty("GEMINI_AI_API_KEY"));
  }

  @Test
  void structuredOutput_e2e(BpmClient client) {
    Ivy.session().loginSessionUser("James", "secret");
    var res = client.start()
        .process(AGENT_TOOLS.elementName("structuredOutput"))
        .as().session(Ivy.session())
        .execute();
    TestToolUserData data = res.data().last();
    assertThat(data.getPerson().getFirstName())
        .isEqualTo("James");
  }
}
