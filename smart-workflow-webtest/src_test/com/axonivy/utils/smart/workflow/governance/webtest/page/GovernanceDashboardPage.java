package com.axonivy.utils.smart.workflow.governance.webtest.page;

import static com.codeborne.selenide.Condition.text;
import com.codeborne.selenide.ElementsCollection;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;
import com.codeborne.selenide.SelenideElement;

public class GovernanceDashboardPage {

  interface Css {
    String TITLE = "h2.text-2xl";
    String SUMMARY_COUNT = ".flex.justify-content-between .font-normal.text-color-secondary";
    String HISTORY_TABLE = "[id$='history-table']";
    String COLUMN_HEADERS = ".history-table thead th";
    String EMPTY_MESSAGE = ".ui-treetable-empty-message";
    String ROW_TOGGLER = ".ui-treetable-toggler";
  }

  interface FilterBar {
    String CASE_INPUT = ".filter-bar .col input[type='text']";
    String MODEL_DROPDOWN = ".filter-bar .col-3 select";
    String DATE_RANGE_DROPDOWN = ".filter-bar .col-2 select";
  }

  interface Row {
    String CASE = "[id$=':case-name']";
    String TASK = "[id$=':task-name']";
    String AGENT = "[id$=':agent-name']";
    String ACTION_BUTTON = "[id*='row-action-btn']";
    String VIEW_DETAILS_MENU_ITEM = ".ui-menu .ui-menuitem-link";
  }

  interface Analytics {
    String KPI_TOTAL_SESSIONS = "[id$='kpi-total-sessions']";
    String KPI_TOTAL_TOKENS = "[id$='kpi-total-tokens']";
    String KPI_AVG_RESPONSE_TIME = "[id$='kpi-avg-response-time']";
    String KPI_TOP_MODEL = "[id$='kpi-top-model']";
    String CHART_PANEL_TOKEN_TIMELINE = "[id$='chart-panel-token-timeline']";
    String CHART_PANEL_MODEL_DIST = "[id$='chart-panel-model-dist']";
    String CHART_PANEL_TOKEN_STACKED = "[id$='chart-panel-token-stacked']";
    String CHART_PANEL_TOP_CASES = "[id$='chart-panel-top-cases']";
    String CHART_PANEL_RESPONSE_TIME = "[id$='chart-panel-response-time']";
  }

  public SelenideElement title() {
    return $(Css.TITLE);
  }

  public SelenideElement summaryCount() {
    return $(Css.SUMMARY_COUNT);
  }

  public SelenideElement filterCaseInput() {
    return $(FilterBar.CASE_INPUT);
  }

  public SelenideElement modelDropdown() {
    return $(FilterBar.MODEL_DROPDOWN);
  }

  public SelenideElement dateRangeDropdown() {
    return $(FilterBar.DATE_RANGE_DROPDOWN);
  }

  public SelenideElement historyTable() {
    return $(Css.HISTORY_TABLE);
  }

  public ElementsCollection columnHeaders() {
    return $$(Css.COLUMN_HEADERS);
  }

  public ElementsCollection caseRows() {
    return $$(Row.CASE);
  }

  public SelenideElement firstCaseRowToggle() {
    return $(Css.HISTORY_TABLE + " " + Css.ROW_TOGGLER);
  }

  public SelenideElement caseRow(String nameContains) {
    return $$(Row.CASE).findBy(text(nameContains)).closest("tr");
  }

  public SelenideElement caseRowToggler(String nameContains) {
    return caseRow(nameContains).$(Css.ROW_TOGGLER);
  }

  public SelenideElement taskRow(String nameContains) {
    return $$(Row.TASK).findBy(text(nameContains)).closest("tr");
  }

  public SelenideElement taskRowToggler(String nameContains) {
    return taskRow(nameContains).$(Css.ROW_TOGGLER);
  }

  public SelenideElement agentRowByName(String nameContains) {
    return $$(Row.AGENT).findBy(text(nameContains)).closest("tr");
  }

  public ElementsCollection togglers() {
    return $$(Css.HISTORY_TABLE + " " + Css.ROW_TOGGLER);
  }

  public ElementsCollection agentRows() {
    return $$(Row.AGENT);
  }

  public SelenideElement firstCaseActionButton() {
    return $(Row.ACTION_BUTTON);
  }

  public SelenideElement viewDetailsMenuItem() {
    return $(Row.VIEW_DETAILS_MENU_ITEM);
  }

  public SelenideElement emptyMessage() {
    return $(Css.EMPTY_MESSAGE);
  }

  public SelenideElement kpiTotalSessions() {
    return $(Analytics.KPI_TOTAL_SESSIONS);
  }

  public SelenideElement kpiTotalTokens() {
    return $(Analytics.KPI_TOTAL_TOKENS);
  }

  public SelenideElement kpiAvgResponseTime() {
    return $(Analytics.KPI_AVG_RESPONSE_TIME);
  }

  public SelenideElement kpiTopModel() {
    return $(Analytics.KPI_TOP_MODEL);
  }

  public SelenideElement chartPanelTokenTimeline() {
    return $(Analytics.CHART_PANEL_TOKEN_TIMELINE);
  }

  public SelenideElement chartPanelModelDist() {
    return $(Analytics.CHART_PANEL_MODEL_DIST);
  }

  public SelenideElement chartPanelTokenStacked() {
    return $(Analytics.CHART_PANEL_TOKEN_STACKED);
  }

  public SelenideElement chartPanelTopCases() {
    return $(Analytics.CHART_PANEL_TOP_CASES);
  }

  public SelenideElement chartPanelResponseTime() {
    return $(Analytics.CHART_PANEL_RESPONSE_TIME);
  }
}
