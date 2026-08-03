package com.axonivy.utils.smart.workflow.governance.webtest.fixture;

import com.axonivy.ivy.webtest.engine.EngineUrl;
import com.axonivy.utils.smart.workflow.governance.webtest.ConversationsMockDataFactory;
import com.axonivy.utils.smart.workflow.governance.webtest.page.ConversationsPage;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.open;

public interface ConversationsFixture extends GovernanceDashboardFixture {

  default void setupConversationsMockData() {
    open(EngineUrl.createProcessUrl(
        "/smart-workflow-webtest/" + SETUP_DATA_PROCESS_ID + "/setupConversationsData.ivp"));
  }

  default void cleanupConversationsMockData() {
    open(EngineUrl.createProcessUrl(
        "/smart-workflow-webtest/" + SETUP_DATA_PROCESS_ID + "/cleanupConversationsData.ivp"));
  }

  default ConversationsPage navigateToConversations() {
    var dashboard = navigateToGovernanceDashboard();
    dashboard.caseRow(ConversationsMockDataFactory.CASE_UUID)
        .find("[id*='row-action-btn']").click();
    dashboard.viewDetailsMenuItem().shouldBe(visible).click();
    return new ConversationsPage();
  }
}
