package com.axonivy.utils.smart.workflow.governance.webtest.fixture;

import com.axonivy.ivy.webtest.engine.EngineUrl;
import com.axonivy.utils.smart.workflow.governance.webtest.page.GovernanceDashboardPage;
import static com.codeborne.selenide.Selenide.open;

public interface GovernanceDashboardFixture {

  String SETUP_DATA_PROCESS_ID = "19AB0001DEAD0001";

  default GovernanceDashboardPage navigateToGovernanceDashboard() {
    open(EngineUrl.createProcessUrl("/smart-workflow/19F0212A34042E00/governance.ivp"));
    return new GovernanceDashboardPage();
  }

  default void setupGovernanceMockData() {
    open(EngineUrl.createProcessUrl(
        "/smart-workflow-webtest/" + SETUP_DATA_PROCESS_ID + "/setupTestData.ivp"));
  }

  default void cleanupGovernanceMockData() {
    open(EngineUrl.createProcessUrl(
        "/smart-workflow-webtest/" + SETUP_DATA_PROCESS_ID + "/cleanupTestData.ivp"));
  }
}
