package com.axonivy.utils.smart.workflow.model.ollama;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.ollama.OllamaContainer;
import org.testcontainers.utility.DockerImageName;

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
@Testcontainers
public class OllamaModelE2E {

  private static final String MODEL_NAME = StringUtils.defaultIfBlank(
      TestUtils.getSystemProperty("OLLAMA_MODEL"), "llama3.2");

  private static final BpmProcess AGENT_TOOLS = BpmProcess.name("TestToolUser");

  @Container
  private static final OllamaContainer ollama = new OllamaContainer(
      DockerImageName.parse("ollama/ollama:latest"));

  @RegisterExtension
  LoggerAccess log = new LoggerAccess(LoggingHttpClient.class.getName());

  @BeforeAll
  static void pullModel() throws IOException, InterruptedException {
    ollama.execInContainer("ollama", "pull", MODEL_NAME);
  }

  @BeforeEach
  void setup(AppFixture fixture) {
    fixture.var(AiConf.DEFAULT_PROVIDER, OllamaModelProvider.NAME);
    fixture.var(OllamaConf.BASE_URL, ollama.getEndpoint());
    fixture.var(OllamaConf.DEFAULT_MODEL, MODEL_NAME);
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
