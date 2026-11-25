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

/** Text inference against Aura, configured with AURA_ORG_ID, AURA_HOST and AURA_APP_ID. */
@IvyProcessTest
@EnabledIf("providerConfigured")
class AuraModelE2E {

  private static final BpmProcess AGENT_TOOLS = BpmProcess.name("TestToolUser");

  @BeforeEach
  void setup(AppFixture fixture) {
    fixture.var(AiConf.DEFAULT_PROVIDER, AuraModelProvider.NAME);
    fixture.var(AuraConf.ORG_ID, TestUtils.getSystemProperty("AURA_ORG_ID"));
    fixture.var(AuraConf.HOST, TestUtils.getSystemProperty("AURA_HOST"));
    fixture.var(AuraConf.APP_ID, TestUtils.getSystemProperty("AURA_APP_ID"));
  }

  @Test
  void textOutput(BpmClient client) {
    Ivy.session().loginSessionUser("James", "secret");
    var result = client.start().process(AGENT_TOOLS.elementName("expandQuery"))
        .as().session(Ivy.session()).execute();
    TestToolUserData data = result.data().last();
    assertThat(data.getResult()).contains("James");
  }

  static boolean providerConfigured() {
    return TestUtils.isProviderEnabled(AuraModelProvider.NAME)
        && StringUtils.isNotBlank(TestUtils.getSystemProperty("AURA_ORG_ID"))
        && StringUtils.isNotBlank(TestUtils.getSystemProperty("AURA_HOST"))
        && StringUtils.isNotBlank(TestUtils.getSystemProperty("AURA_APP_ID"));
  }
}
