package com.axonivy.utils.smart.workflow.demo.supplier.onboarding.bean;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.axonivy.utils.smart.workflow.demo.supplier.agent.SupplierAgentResponse;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.agent.AgentProcessingStep;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.enums.AgentStepStatus;

import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
class TestSupplierAgentProcessingBean {

  @ParameterizedTest(name = "{0}")
  @CsvSource(value = {
      "isAnalysisComplete_whenRoutingDecisionSet_returnsTrue,    APPROVAL, true",
      "isAnalysisComplete_whenRoutingDecisionNull_returnsFalse,  NULL,     false",
      "isAnalysisComplete_whenRoutingDecisionEmpty_returnsFalse, '',       false"
  }, nullValues = "NULL")
  void isAnalysisComplete(String testName, String routingDecision, boolean expected) throws Exception {
    SupplierAgentProcessingBean bean = new SupplierAgentProcessingBean();
    SupplierAgentResponse response = new SupplierAgentResponse();
    response.setRoutingDecision(routingDecision);
    setAgentResponse(bean, response);
    assertThat(bean.isAnalysisComplete()).as(testName).isEqualTo(expected);
  }

  @ParameterizedTest(name = "{0}")
  @CsvSource({
      "isHasFailedStep_whenStepFailed_returnsTrue,    FAILED,    true",
      "isHasFailedStep_whenAllCompleted_returnsFalse, COMPLETED, false"
  })
  void isHasFailedStep(String testName, AgentStepStatus status, boolean expected) throws Exception {
    SupplierAgentProcessingBean bean = new SupplierAgentProcessingBean();
    SupplierAgentResponse response = new SupplierAgentResponse();
    AgentProcessingStep step = new AgentProcessingStep();
    step.setStatus(status);
    response.setProcessingSteps(new ArrayList<>(List.of(step)));
    setAgentResponse(bean, response);
    assertThat(bean.isHasFailedStep()).as(testName).isEqualTo(expected);
  }

  @Test
  void defaultState_whenAgentResponseNull() {
    SupplierAgentProcessingBean bean = new SupplierAgentProcessingBean();
    assertThat(bean.isAnalysisComplete()).isFalse();
    assertThat(bean.isHasFailedStep()).isFalse();
    assertThat(bean.getContinueButtonClass()).isEqualTo("ui-button-primary");
  }

  @ParameterizedTest(name = "{0}")
  @CsvSource({
      "getContinueButtonClass_returnsSuccess_forApprovalDecision, APPROVAL, ui-button-primary",
      "getContinueButtonClass_returnsDanger_forDeclineDecision,   DECLINE,  ui-button-danger"
  })
  void getContinueButtonClass(String testName, String routingDecision, String expected) throws Exception {
    SupplierAgentProcessingBean bean = new SupplierAgentProcessingBean();
    SupplierAgentResponse response = new SupplierAgentResponse();
    response.setRoutingDecision(routingDecision);
    setAgentResponse(bean, response);
    assertThat(bean.getContinueButtonClass()).as(testName).isEqualTo(expected);
  }

  @ParameterizedTest(name = "{0}")
  @CsvSource({
      "getDecisionBoxCssClass_forApproval_returnsSuccessBanner,  APPROVAL,      so-success-banner",
      "getDecisionBoxCssClass_forDecline_returnsDeclineBanner,   DECLINE,       so-decline-banner",
      "getDecisionBoxCssClass_forClarification_returnsEmpty,     CLARIFICATION, ''"
  })
  void getDecisionBoxCssClass(String testName, String routingDecision, String expected) throws Exception {
    SupplierAgentProcessingBean bean = new SupplierAgentProcessingBean();
    SupplierAgentResponse response = new SupplierAgentResponse();
    response.setRoutingDecision(routingDecision);
    setAgentResponse(bean, response);
    assertThat(bean.getDecisionBoxCssClass()).as(testName).isEqualTo(expected);
  }

  @ParameterizedTest(name = "{0}")
  @CsvSource(value = {
      "getStepStatusClass_whenCompleted_returnsGreen,   COMPLETED, text-green-600",
      "getStepStatusClass_whenRunning_returnsBlue,      RUNNING,   text-blue-600",
      "getStepStatusClass_whenFailed_returnsRed,        FAILED,    text-red-600",
      "getStepStatusClass_whenPending_returnsSecondary, PENDING,   text-color-secondary",
      "getStepStatusClass_whenNull_returnsSecondary,    NULL,      text-color-secondary"
  }, nullValues = "NULL")
  void getStepStatusClass(String testName, AgentStepStatus status, String expected) {
    SupplierAgentProcessingBean bean = new SupplierAgentProcessingBean();
    assertThat(bean.getStepStatusClass(status == null ? null : step(status))).as(testName).isEqualTo(expected);
  }

  private static void setAgentResponse(SupplierAgentProcessingBean bean,
      SupplierAgentResponse response) throws Exception {
    var field = SupplierAgentProcessingBean.class.getDeclaredField("agentResponse");
    field.setAccessible(true);
    field.set(bean, response);
  }

  private static AgentProcessingStep step(AgentStepStatus status) {
    AgentProcessingStep step = new AgentProcessingStep();
    step.setStatus(status);
    return step;
  }
}
