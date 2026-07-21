package com.axonivy.utils.smart.workflow.governance.webtest;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.axonivy.ivy.webtest.IvyWebTest;
import com.axonivy.ivy.webtest.engine.WebAppFixture;
import com.axonivy.utils.smart.workflow.governance.webtest.fixture.GovernanceDashboardFixture;
import com.axonivy.utils.smart.workflow.governance.webtest.fixture.LoginFixture;
import com.axonivy.utils.smart.workflow.governance.webtest.page.GovernanceDashboardPage;
import com.codeborne.selenide.SelenideElement;
import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.CollectionCondition.sizeGreaterThan;
import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$$;

@IvyWebTest(browser="chrome")
class GovernanceDashboardWebTest implements LoginFixture, GovernanceDashboardFixture {

  private GovernanceDashboardPage page;

  @BeforeEach
  public void setup(WebAppFixture fixture) {
    login(fixture);
    cleanupGovernanceMockData();
  }

  @Test
  void dashboard_empty() {
    page = navigateToGovernanceDashboard();

    page.title().shouldBe(visible);
    page.title().shouldHave(text("AI Governance Center"));

    page.filterCaseInput().shouldBe(visible);
    page.modelDropdown().should(exist);
    page.dateRangeDropdown().should(exist);

    page.historyTable().shouldBe(visible);
    page.columnHeaders().get(0).shouldHave(text("Case/Task/Agent"));
    page.columnHeaders().get(1).shouldHave(text("Last updated"));
    page.columnHeaders().get(2).shouldHave(text("Messages"));
    page.columnHeaders().get(3).shouldHave(text("Tokens"));
    page.columnHeaders().get(4).shouldHave(text("Model"));
    page.columnHeaders().get(5).shouldHave(text("Actions"));

    assertTableState(0, 0);
    page.emptyMessage().shouldBe(visible);
    page.emptyMessage().shouldHave(text("No history records found."));
  }

  @Test
  void dashboard_withData() {
    setupGovernanceMockData();
    page = navigateToGovernanceDashboard();

    assertTableState(3, 7);

    page.firstCaseActionButton().click();
    page.viewDetailsMenuItem().shouldBe(visible);
    page.viewDetailsMenuItem().shouldHave(text("View details"));

    var case1Row = page.caseRow("webtest-case-001");
    assertRowCells(case1Row, "webtest-case-001-agent-pipeline", "15 msgs", "4355", "gpt-4.1-mini-2025-04-14");
    page.caseRowToggler("webtest-case-001").click();
    $$(".history-table tbody tr").shouldHave(sizeGreaterThan(3), Duration.ofSeconds(5));

    var task1aRow = page.taskRow("webtest-task-001a");
    assertRowCells(task1aRow, "webtest-task-001a-image", "3 msgs", "1612", "gpt-4.1-mini-2025-04-14");
    page.taskRowToggler("webtest-task-001a").click();
    page.agentRows().shouldHave(sizeGreaterThan(0), Duration.ofSeconds(5));

    var agentRow = page.agentRowByName("Extract Invoice Content from Image");
    assertRowCells(agentRow, "Extract Invoice Content from Image", "3 msgs", "1612", "gpt-4.1-mini-2025-04-14");

    page.dateRangeDropdown().selectOptionByValue("TODAY");
    assertTableState(2, 6);

    page.modelDropdown().selectOption(1);
    page.summaryCount().shouldBe(visible, Duration.ofSeconds(5));
  }

  @Test
  void dashboard_filters() {
    setupGovernanceMockData();
    page = navigateToGovernanceDashboard();

    page.modelDropdown().selectOptionByValue("claude-opus-4-7");
    page.emptyMessage().shouldBe(visible, Duration.ofSeconds(5));

    page.modelDropdown().selectOptionByValue("gpt-4.1-mini-2025-04-14");
    assertTableState(3, 7);

    page = navigateToGovernanceDashboard();
    page.dateRangeDropdown().selectOptionByValue("ALL");
    assertTableState(4, 8);

    page.dateRangeDropdown().selectOptionByValue("LAST_30_DAYS");
    assertTableState(3, 7);

    page.dateRangeDropdown().selectOptionByValue("TODAY");
    assertTableState(2, 6);

    page.dateRangeDropdown().selectOptionByValue("ALL");
    page.filterCaseInput().setValue("case-001-agent-pipeline");
    page.filterCaseInput().pressEnter();
    page.caseRows().shouldHave(size(1), Duration.ofSeconds(5));

    page.filterCaseInput().clear();
    page.filterCaseInput().pressEnter();
    page.caseRows().shouldHave(size(4), Duration.ofSeconds(5));
  }

  private void assertTableState(int caseCount, int taskCount) {
    page.caseRows().shouldHave(size(caseCount), Duration.ofSeconds(5));
    page.summaryCount().shouldHave(text(caseCount + " cases - " + taskCount + " tasks"), Duration.ofSeconds(5));
  }

  private void assertRowCells(SelenideElement row, String name, String msgs, String tokens, String model) {
    row.$$("td").get(0).shouldHave(text(name));
    row.$$("td").get(2).shouldHave(text(msgs));
    row.$$("td").get(3).shouldHave(text(tokens));
    row.$$("td").get(4).shouldHave(text(model));
  }
}
