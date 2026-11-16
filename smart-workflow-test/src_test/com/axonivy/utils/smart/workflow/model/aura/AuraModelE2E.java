package com.axonivy.utils.smart.workflow.model.aura;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import com.axonivy.utils.smart.workflow.model.ChatModelFactory.AiConf;
import com.axonivy.utils.smart.workflow.model.aura.internal.AuraServiceConnector.AuraConf;
import com.axonivy.utils.smart.workflow.test.TestToolUserData;
import com.axonivy.utils.smart.workflow.test.utils.TestUtils;

import ch.ivyteam.ivy.bpm.engine.client.BpmClient;
import ch.ivyteam.ivy.bpm.engine.client.element.BpmProcess;
import ch.ivyteam.ivy.bpm.exec.client.IvyProcessTest;
import ch.ivyteam.ivy.environment.AppFixture;
import ch.ivyteam.ivy.environment.Ivy;

/** End-to-end agent flows configured with AURA_BASE_URL, AURA_MODEL and AURA_API_KEY. */
@IvyProcessTest
@EnabledIf("providerConfigured")
class AuraModelE2E {

  private static final BpmProcess AGENT_TOOLS = BpmProcess.name("TestToolUser");

  @BeforeEach
  void setup(AppFixture fixture) {
    fixture.var(AiConf.DEFAULT_PROVIDER, AuraModelProvider.NAME);
    fixture.var(AuraConf.BASE_URL, TestUtils.getSystemProperty("AURA_BASE_URL"));
    fixture.var(AuraConf.DEFAULT_MODEL, TestUtils.getSystemProperty("AURA_MODEL"));
    fixture.var(AuraConf.API_KEY, TestUtils.getSystemProperty("AURA_API_KEY"));
  }

  @Test
  void textOutput(BpmClient client) {
    Ivy.session().loginSessionUser("James", "secret");
    var result = client.start().process(AGENT_TOOLS.elementName("expandQuery"))
        .as().session(Ivy.session()).execute();
    TestToolUserData data = result.data().last();
    assertThat(data.getResult()).contains("James");
  }

  @Test
  void structuredOutput(BpmClient client) {
    var result = client.start().process(AGENT_TOOLS.elementName("structuredOutputList")).execute();
    TestToolUserData data = result.data().last();
    assertThat(data.getStory()).isNotNull();
    assertThat(data.getStory().getTitle()).isNotBlank();
  }

  @Test
  void structuredOutputWithTool(BpmClient client) {
    Ivy.session().loginSessionUser("James", "secret");
    var result = client.start().process(AGENT_TOOLS.elementName("structuredOutput"))
        .as().session(Ivy.session()).execute();
    TestToolUserData data = result.data().last();
    assertThat(data.getPerson().getFirstName()).isEqualTo("James");
  }

  static boolean providerConfigured() {
    return TestUtils.isProviderEnabled(AuraModelProvider.NAME)
        && StringUtils.isNotBlank(TestUtils.getSystemProperty("AURA_BASE_URL"))
        && StringUtils.isNotBlank(TestUtils.getSystemProperty("AURA_MODEL"))
        && StringUtils.isNotBlank(TestUtils.getSystemProperty("AURA_API_KEY"));
  }
}
