package com.axonivy.utils.smart.workflow.guardrails;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import Features.GuardrailDemoData;
import ch.ivyteam.ivy.bpm.engine.client.BpmClient;
import ch.ivyteam.ivy.bpm.engine.client.element.BpmProcess;
import ch.ivyteam.ivy.bpm.exec.client.IvyProcessTest;
import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.test.log.LoggerAccess;
import dev.langchain4j.http.client.log.LoggingHttpClient;

@IvyProcessTest
public class TestGuardrailProcess {
  private static final BpmProcess GUARDRAIL_DEMO = BpmProcess.name("GuardrailDemo");

  @RegisterExtension
  LoggerAccess log = new LoggerAccess(LoggingHttpClient.class.getName());

  @Test
  void testCatchPromptInjection(BpmClient client) {
    Ivy.session().loginSessionUser("James", "secret");
    var res = client.start()
        .process(GUARDRAIL_DEMO.elementName("Prompt Injection Guardrail Demo"))
        .as().session(Ivy.session())
        .execute();
    GuardrailDemoData data = res.data().last();
    assertThat(data.getResult().contains(
        "PromptInjectionGuardrail: The input message is rejected because it's empty or contains malicious content"));
  }
}
