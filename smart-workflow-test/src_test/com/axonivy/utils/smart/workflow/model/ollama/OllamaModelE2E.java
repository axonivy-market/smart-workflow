package com.axonivy.utils.smart.workflow.model.ollama;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.axonivy.utils.smart.workflow.model.ChatModelFactory.AiConf;
import com.axonivy.utils.smart.workflow.model.ollama.internal.OllamaServiceConnector.OllamaConf;
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
public class OllamaModelE2E {

  private static final BpmProcess AGENT_TOOLS = BpmProcess.name("TestToolUser");

  @RegisterExtension
  LoggerAccess log = new LoggerAccess(LoggingHttpClient.class.getName());

  @BeforeEach
  void setup(AppFixture fixture) {
    fixture.var(AiConf.DEFAULT_PROVIDER, OllamaModelProvider.NAME);
    fixture.var(OllamaConf.BASE_URL,
        StringUtils.defaultIfBlank(TestUtils.getSystemProperty("OLLAMA_BASE_URL"), "http://localhost:11434"));
    fixture.var(OllamaConf.DEFAULT_MODEL,
      StringUtils.defaultIfBlank(TestUtils.getSystemProperty("OLLAMA_MODEL"), "llama3.2"));
  }

  @Test
  void chatOutput_e2e(BpmClient client) {
    Ivy.session().loginSessionUser("James", "secret");
    var res = client.start()
        .process(AGENT_TOOLS.elementName("systemMessage"))
        .as().session(Ivy.session())
        .execute();
    TestToolUserData data = res.data().last();
    assertThat(StringUtils.isNotBlank(data.getResult())).isTrue();
  }
}
