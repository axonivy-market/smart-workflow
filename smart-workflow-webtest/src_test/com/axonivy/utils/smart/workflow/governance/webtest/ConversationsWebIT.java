package com.axonivy.utils.smart.workflow.governance.webtest;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.axonivy.ivy.webtest.IvyWebTest;
import com.axonivy.ivy.webtest.engine.WebAppFixture;
import com.axonivy.utils.smart.workflow.governance.webtest.ConversationsMockDataFactory.AiReport;
import com.axonivy.utils.smart.workflow.governance.webtest.fixture.ConversationsFixture;
import com.axonivy.utils.smart.workflow.governance.webtest.fixture.LoginFixture;
import com.axonivy.utils.smart.workflow.governance.webtest.page.ConversationsPage;
import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;

@IvyWebTest(browser = "chrome", headless=false)
class ConversationsWebIT implements LoginFixture, ConversationsFixture {

  private ConversationsPage page;

  @BeforeEach
  public void setup(WebAppFixture fixture) {
    login(fixture);
    cleanupGovernanceMockData();
    setupConversationsMockData();
  }

  @Test
  void conversations() {
    page = navigateToConversations();
    verifySummary();
    verifyOcrAutoExpanded();
    verifyHeaderMessageTabs();
    verifyAnalyzerToolsTab();
  }

  @Test
  void analysisReport() {
    page = navigateToConversations();
    page.contentPanel().shouldBe(visible);
    page.reportPanel().shouldBe(visible, Duration.ofSeconds(5));
    page.expandReportPanel();

    verifyStatisticTab();
    verifyAiRecommendationTab();
  }

  private void verifyStatisticTab() {
    var tab = page.openReportTab(ConversationsPage.ReportTab.STATISTIC);

    tab.shouldHave(text(ConversationsMockDataFactory.CASE_UUID));
    tab.shouldHave(text(ConversationsMockDataFactory.Meta.PROCESS));

    // one grade badge per agent in the breakdown
    page.agentGradeBadges(tab).shouldHave(size(3), Duration.ofSeconds(5));

    // tool effectiveness table lists the mocked tool calls
    tab.shouldHave(text(ConversationsMockDataFactory.ToolName.EXTRACT_HEADER));
    tab.shouldHave(text(ConversationsMockDataFactory.ToolName.EXTRACT_ITEMS));
  }

  private void verifyAiRecommendationTab() {
    var tab = page.openReportTab(ConversationsPage.ReportTab.AI_RECOMMENDATION);

    // a stored report replaces the generate button - clicking it would invoke a real LLM
    page.aiGenerateButtonWrap().shouldNot(exist);

    // 1. executive summary
    tab.shouldHave(text(AiReport.SUMMARY));

    // 2. risk assessment - Moderate is sorted ahead of the Low entries
    page.riskBadges(ConversationsPage.Css.RISK_BADGE_HIGH)
        .shouldHave(size(AiReport.HIGH_RISK_COUNT));
    page.riskBadges(ConversationsPage.Css.RISK_BADGE_MODERATE)
        .shouldHave(size(AiReport.MODERATE_RISK_COUNT));
    page.riskBadges(ConversationsPage.Css.RISK_BADGE_LOW)
        .shouldHave(size(AiReport.LOW_RISK_COUNT));

    // 3. efficiency opportunities
    tab.shouldHave(text(ConversationsMockDataFactory.AgentName.ANALYZER));
    tab.shouldHave(text(AiReport.EFFICIENCY_OBSERVATION));
    page.efficiencySuggestions().shouldBe(visible)
        .shouldHave(text(AiReport.EFFICIENCY_SUGGESTION));

    // 4. reliability concerns + tool usage patterns
    tab.shouldHave(text(AiReport.ANOMALY));
    tab.shouldHave(text(AiReport.RELIABILITY_CONCLUSION));
    page.toolChips(tab).shouldHave(size(AiReport.TOOLS_USED_COUNT));
    tab.shouldHave(text(AiReport.TOOL_OBSERVATION));
    tab.shouldHave(text(AiReport.TOOL_INSIGHT));

    // 5. recommendations
    tab.shouldHave(text(AiReport.RECOMMENDATION_PERFORMANCE));
    tab.shouldHave(text(AiReport.RECOMMENDATION_DATA_QUALITY));
  }

  private void verifySummary() {
    page.contentPanel().shouldBe(visible);
    page.taskSeparators().shouldHave(size(2), Duration.ofSeconds(5));
    page.agentBlocks().shouldHave(size(3), Duration.ofSeconds(5));
    page.summaryKpiValues().get(0).shouldHave(text("2"));
  }

  private void verifyOcrAutoExpanded() {
    var ocrBlock = page.agentBlock(ConversationsMockDataFactory.AgentName.OCR);
    ocrBlock.shouldBe(visible, Duration.ofSeconds(5));
    ocrBlock.shouldHave(cssClass(ConversationsPage.Css.AGENT_OPEN));
  }

  private void verifyHeaderMessageTabs() {
    var headerBlock = page.agentBlock(ConversationsMockDataFactory.AgentName.HEADER);
    page.agentHeader(headerBlock).click();
    headerBlock.shouldHave(cssClass(ConversationsPage.Css.AGENT_OPEN), Duration.ofSeconds(5));

    page.clickTab(headerBlock, 1);
    page.systemTabContent(headerBlock).shouldBe(visible)
        .shouldHave(text("invoice header extraction specialist"));
    page.clickTab(headerBlock, 2);
    page.inputTabContent(headerBlock).shouldBe(visible)
        .shouldHave(text("Extract header information"));
    page.clickTab(headerBlock, 3);
    page.responseTabContent(headerBlock).shouldBe(visible)
        .shouldHave(text("INV-0001-0001"));
  }

  private void verifyAnalyzerToolsTab() {
    var analyzerBlock = page.agentBlock(ConversationsMockDataFactory.AgentName.ANALYZER);
    page.agentHeader(analyzerBlock).click();
    analyzerBlock.shouldHave(cssClass(ConversationsPage.Css.AGENT_OPEN), Duration.ofSeconds(5));

    page.clickTab(analyzerBlock, 4);
    page.toolsTabContent(analyzerBlock).shouldBe(visible);

    var cards = page.toolCards(analyzerBlock);
    cards.shouldHave(size(2), Duration.ofSeconds(5));

    var card1 = cards.get(0);
    page.toolCardName(card1).shouldHave(text(ConversationsMockDataFactory.ToolName.EXTRACT_HEADER));
    page.expandToolCard(card1);
    page.toolCardResultBody(card1).shouldBe(visible).shouldHave(text("INV-0001-0001"));
    page.expandToolCardInput(card1);
    page.toolCardInputBody(card1).shouldBe(visible);
    page.toolCardInputKey(card1, 0).shouldHave(text("invoiceContent"));

    var card2 = cards.get(1);
    page.toolCardName(card2).shouldHave(text(ConversationsMockDataFactory.ToolName.EXTRACT_ITEMS));
    page.expandToolCard(card2);
    page.toolCardResultBody(card2).shouldBe(visible).shouldHave(text("Software License"));
  }
}
