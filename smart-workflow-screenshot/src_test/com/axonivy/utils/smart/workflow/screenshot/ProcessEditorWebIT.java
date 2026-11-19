package com.axonivy.utils.smart.workflow.screenshot;

import static com.axonivy.utils.smart.workflow.screenshot.ProcessEditor.getEditorUrl;
import static com.axonivy.utils.smart.workflow.screenshot.ProcessEditor.openInscription;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.open;
import static com.codeborne.selenide.Selenide.screenshot;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import com.axonivy.ivy.webtest.IvyWebTest;

@IvyWebTest
class ProcessEditorWebIT {

  private static final String PROCESS_ID = "1987FC3259488F27";

  @Test
  void screenshotConfiguration() {
    var editorUrl = getEditorUrl("smart-workflow-test", "process/TestToolUser.p.json");

    open(editorUrl);
    $("svg.sprotty-graph").shouldBe(visible, Duration.ofSeconds(60));

    captureConfigurationGroup(PROCESS_ID + "-f61", "Message");
    screenshot("TestToolUser-message");
    captureConfigurationGroup(PROCESS_ID + "-f66", "Tools");
    screenshot("TestToolUser-tools");
    captureConfigurationGroup(PROCESS_ID + "-f71", "Guardrails");
    screenshot("TestToolUser-guardrails");
    captureConfigurationGroup(PROCESS_ID + "-f22", "Model");
    screenshot("TestToolUser-model");
    captureConfigurationGroup(PROCESS_ID + "-f11", "Output");
    screenshot("TestToolUser-output");
  }

  public static void captureConfigurationGroup(String elementId, String groupName) {
    var inscription = openInscription(elementId);
    inscription.openTab("Configuration");
    if (!groupName.equals("Message")) {
      inscription.expandGroup("Message", false);
    }
    inscription.expandGroup(groupName, true);
  }

}