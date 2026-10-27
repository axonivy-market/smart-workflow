package com.axonivy.utils.smart.workflow.governance.webtest.page;

import java.time.Duration;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import com.codeborne.selenide.ElementsCollection;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;
import com.codeborne.selenide.SelenideElement;

public class ConversationsPage {

  public interface Css {
    String CONTENT_PANEL  = "[id$='cv-content-panel']";
    String EMPTY_STATE    = ".cv-empty-state";
    String TASK_SEP       = ".cv-tl-task-sep";
    String AGENT_BLOCK    = ".cv-agent-block";
    String AGENT_HEADER   = ".cv-agent-header";
    String AGENT_OPEN     = "cv-agent-open";
    String TAB_NAV        = ".cv-agent-tabview .ui-tabs-nav";
    String TAB_SYSTEM     = ".cv-tab-content--system";
    String TAB_INPUT      = ".cv-tab-content--input";
    String TAB_RESPONSE   = ".cv-tab-content--response";
    String TAB_TOOLS      = ".cv-tab-content--tools";
    String TOOL_CARD          = ".tool-timeline-card";
    String TOOL_CARD_HEADER   = ".tool-timeline-card-header";
    String TOOL_CARD_NAME     = ".tool-timeline-card-header span.flex-1";
    String TOOL_RESULT_BODY   = ".tool-timeline-result-body";
    String TOOL_INPUT_TOGGLE  = ".tool-timeline-input-toggle";
    String TOOL_INPUT_BODY    = ".tool-timeline-input-body";
    String TOOL_INPUT_KEY     = ".tool-timeline-kv-key";
    String KPI_VALUES         = ".text-2xl.font-bold.text-900";

    String REPORT_PANEL        = ".cv-report-panel";
    String REPORT_PANEL_TOGGLE = ".cv-report-panel .ui-panel-titlebar-icon";
    String REPORT_PANEL_BODY   = ".cv-report-panel .ui-panel-content";
    String REPORT_TAB_NAV      = ".cv-report-tabs .ui-tabs-nav";
    String REPORT_TAB_PANEL    = ".cv-report-tabs .ui-tabs-panel";
    String AI_GEN_BTN_WRAP     = "#cv-ai-gen-btn-wrap";
    String RISK_BADGE_HIGH     = ".cv-risk-badge--high";
    String RISK_BADGE_MODERATE = ".cv-risk-badge--moderate";
    String RISK_BADGE_LOW      = ".cv-risk-badge--low";
    String OBS_DOT             = ".cv-obs-dot";
    String SUGGESTIONS         = ".cv-suggestions-indent";
    String GRADE_BADGE         = ".cv-status-badge";
    String TOOL_CHIP           = "code";
  }

  /** 1-based tab positions inside the Analysis Report panel. */
  public interface ReportTab {
    int STATISTIC         = 1;
    int AI_RECOMMENDATION = 2;
  }

  public SelenideElement contentPanel() {
    return $(Css.CONTENT_PANEL);
  }

  public SelenideElement emptyState() {
    return $(Css.EMPTY_STATE);
  }

  public ElementsCollection taskSeparators() {
    return $$(Css.TASK_SEP);
  }

  public ElementsCollection agentBlocks() {
    return $$(Css.AGENT_BLOCK);
  }

  public SelenideElement agentBlock(String agentNameContains) {
    return $$(Css.AGENT_BLOCK).findBy(text(agentNameContains));
  }

  public SelenideElement agentHeader(SelenideElement block) {
    return block.$(Css.AGENT_HEADER);
  }

  /** Clicks the nth tab (1-based) inside the given agent block. */
  public void clickTab(SelenideElement block, int tabIndex) {
    block.$(Css.TAB_NAV + " li:nth-child(" + tabIndex + ") a").click();
  }

  public SelenideElement systemTabContent(SelenideElement block) {
    return block.$(Css.TAB_SYSTEM);
  }

  public SelenideElement inputTabContent(SelenideElement block) {
    return block.$(Css.TAB_INPUT);
  }

  public SelenideElement responseTabContent(SelenideElement block) {
    return block.$(Css.TAB_RESPONSE);
  }

  public SelenideElement toolsTabContent(SelenideElement block) {
    return block.$(Css.TAB_TOOLS);
  }

  public ElementsCollection toolCards(SelenideElement block) {
    return block.$$(Css.TOOL_CARD);
  }

  public SelenideElement toolCardName(SelenideElement toolCard) {
    return toolCard.$(Css.TOOL_CARD_NAME);
  }

  public void expandToolCard(SelenideElement toolCard) {
    toolCard.$(Css.TOOL_CARD_HEADER).click();
  }

  public SelenideElement toolCardResultBody(SelenideElement toolCard) {
    return toolCard.$(Css.TOOL_RESULT_BODY);
  }

  public void expandToolCardInput(SelenideElement toolCard) {
    toolCard.$(Css.TOOL_INPUT_TOGGLE).click();
  }

  public SelenideElement toolCardInputBody(SelenideElement toolCard) {
    return toolCard.$(Css.TOOL_INPUT_BODY);
  }

  public SelenideElement toolCardInputKey(SelenideElement toolCard, int index) {
    return toolCard.$$(Css.TOOL_INPUT_KEY).get(index);
  }

  /** Returns all KPI value elements (task count, messages, tokens, avg duration). */
  public ElementsCollection summaryKpiValues() {
    return $$(Css.KPI_VALUES);
  }

  // ── Analysis Report panel ──────────────────────────────────────────────

  public SelenideElement reportPanel() {
    return $(Css.REPORT_PANEL);
  }

  /** The report panel renders collapsed; this expands it and waits for the body. */
  public SelenideElement expandReportPanel() {
    $(Css.REPORT_PANEL_TOGGLE).click();
    return $(Css.REPORT_PANEL_BODY).shouldBe(visible, Duration.ofSeconds(5));
  }

  /** Clicks the nth report tab (1-based, see {@link ReportTab}). */
  public void clickReportTab(int tabIndex) {
    $(Css.REPORT_TAB_NAV + " li:nth-child(" + tabIndex + ") a").click();
  }

  /** Content panel of the nth report tab (1-based, see {@link ReportTab}). */
  public SelenideElement reportTabContent(int tabIndex) {
    return $$(Css.REPORT_TAB_PANEL).get(tabIndex - 1);
  }

  public SelenideElement openReportTab(int tabIndex) {
    clickReportTab(tabIndex);
    return reportTabContent(tabIndex).shouldBe(visible, Duration.ofSeconds(5));
  }

  public ElementsCollection riskBadges(String levelCss) {
    return $$(levelCss);
  }

  public ElementsCollection efficiencyObservationDots() {
    return $$(Css.OBS_DOT);
  }

  public SelenideElement efficiencySuggestions() {
    return $(Css.SUGGESTIONS);
  }

  /** The generate button only renders when no AI report is stored for the case. */
  public SelenideElement aiGenerateButtonWrap() {
    return $(Css.AI_GEN_BTN_WRAP);
  }

  public ElementsCollection agentGradeBadges(SelenideElement statisticTab) {
    return statisticTab.$$(Css.GRADE_BADGE);
  }

  public ElementsCollection toolChips(SelenideElement container) {
    return container.$$(Css.TOOL_CHIP);
  }
}
