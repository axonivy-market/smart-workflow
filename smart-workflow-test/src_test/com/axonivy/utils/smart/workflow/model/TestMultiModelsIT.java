package com.axonivy.utils.smart.workflow.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.axonivy.utils.smart.workflow.model.ChatModelFactory.AiConf;
import com.axonivy.utils.smart.workflow.model.azureopenai.AzureOpenAiModelProvider;
import com.axonivy.utils.smart.workflow.model.azureopenai.internal.AzureOpenAiServiceConnector.AzureOpenAiConf;
import com.axonivy.utils.smart.workflow.model.gemini.internal.GeminiServiceConnector;
import com.axonivy.utils.smart.workflow.model.openai.OpenAiModelProvider;
import com.axonivy.utils.smart.workflow.model.openai.internal.OpenAiServiceConnector.OpenAiConf;
import com.axonivy.utils.smart.workflow.test.MathData;
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
public class TestMultiModelsIT {

  private static final BpmProcess AGENT_TOOLS = BpmProcess.name("TestToolUser");

  @RegisterExtension
  LoggerAccess log = new LoggerAccess(LoggingHttpClient.class.getName());

  @BeforeEach
  void setup(AppFixture fixture) {
    fixture.var(AiConf.DEFAULT_PROVIDER, OpenAiModelProvider.NAME); // enforce OpenAI!

    // OpenAI
    fixture.var(OpenAiConf.API_KEY, TestUtils.getSystemProperty("OPEN_AI_API_KEY"));

    // Gemini
    fixture.var(GeminiServiceConnector.GeminiConf.API_KEY, TestUtils.getSystemProperty("GEMINI_AI_API_KEY"));

    // Azure OpenAI
    String deployment = TestUtils.getSystemProperty("AZURE_OPEN_AI_DEPLOYMENT");
    fixture.var(AiConf.DEFAULT_PROVIDER, AzureOpenAiModelProvider.NAME);
    fixture.var(AzureOpenAiConf.DEFAULT_DEPLOYMENT, deployment);
    fixture.var(AzureOpenAiConf.ENDPOINT, TestUtils.getSystemProperty("AZURE_OPEN_AI_ENDPOINT"));
    fixture.var(AzureOpenAiConf.DEPLOYMENTS + "." + deployment + ".Model",
        TestUtils.getSystemProperty("AZURE_OPEN_AI_MODEL"));
    fixture.var(AzureOpenAiConf.DEPLOYMENTS + "." + deployment + ".APIKey",
        TestUtils.getSystemProperty("AZURE_OPEN_AI_API_KEY"));
  }

  @Test
  void chatOutput_e2e(BpmClient client) {
    Ivy.session().loginSessionUser("James", "secret");
    var res = client.start().process(AGENT_TOOLS.elementName("multiAgentsDoMath")).as().session(Ivy.session())
        .execute();
    TestToolUserData data = res.data().last();
    assertThat(Optional.ofNullable(data.getMathObj()).map(MathData::getMathResult).orElse(-1) == 4);
  }
}
