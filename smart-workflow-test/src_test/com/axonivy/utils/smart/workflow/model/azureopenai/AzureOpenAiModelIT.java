package com.axonivy.utils.smart.workflow.model.azureopenai;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.axonivy.utils.smart.workflow.model.ChatModelFactory.AiConf;
import com.axonivy.utils.smart.workflow.model.azureopenai.internal.AzureOpenAiServiceConnector.AzureOpenAiConf;
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
public class AzureOpenAiModelIT {

  private static final BpmProcess AGENT_TOOLS = BpmProcess.name("TestToolUser");

  @RegisterExtension
  LoggerAccess log = new LoggerAccess(LoggingHttpClient.class.getName());

  @BeforeEach
  void setup(AppFixture fixture) {
    fixture.var(AiConf.DEFAULT_PROVIDER, AzureOpenAiModelProvider.NAME);
    fixture.var(AzureOpenAiConf.ENDPOINT, TestUtils.getSystemProperty("AZURE_OPEN_AI_ENDPOINT"));
    fixture.var(AzureOpenAiConf.DEPLOYMENTS + TestUtils.getSystemProperty("AZURE_OPEN_AI_DEPLOYMENT") + ".Model",
        TestUtils.getSystemProperty("AZURE_OPEN_AI_MODEL"));
    fixture.var(AzureOpenAiConf.DEPLOYMENTS + TestUtils.getSystemProperty("AZURE_OPEN_AI_DEPLOYMENT") + ".APIKey",
        TestUtils.getSystemProperty("AZURE_OPEN_AI_API_KEY"));
  }

  @Test
  void structuredOutput_e2e(BpmClient client) {
    Ivy.session().loginSessionUser("James", "secret");
    var res = client.start().process(AGENT_TOOLS.elementName("structuredOutput")).as().session(Ivy.session()).execute();
    TestToolUserData data = res.data().last();
    assertThat(data.getPerson().getFirstName()).isEqualTo("James");
  }
}
