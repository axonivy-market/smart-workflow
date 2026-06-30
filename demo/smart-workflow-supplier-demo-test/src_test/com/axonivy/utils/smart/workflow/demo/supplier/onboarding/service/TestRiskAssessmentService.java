package com.axonivy.utils.smart.workflow.demo.supplier.onboarding.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.axonivy.utils.smart.workflow.demo.supplier.agent.RiskScoreResult;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.agent.AgentProcessingStep;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.builder.SupplierRiskScoreBuilder;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.enums.AgentStepStatus;

import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
class TestRiskAssessmentService {

  @Test
  void startRiskStep_statusIsRunning() {
    AgentProcessingStep step = RiskAssessmentService.startRiskStep();

    assertThat(step.getStatus()).isEqualTo(AgentStepStatus.RUNNING);
    assertThat(step.getStartedAt()).isNotNull();
  }

  @Test
  void finalizeRiskStep_setsStatusCompleted() {
    AgentProcessingStep step = RiskAssessmentService.startRiskStep();

    RiskAssessmentService.finalizeRiskStep(step, null);

    assertThat(step.getStatus()).isEqualTo(AgentStepStatus.COMPLETED);
    assertThat(step.getCompletedAt()).isNotNull();
  }

  @Test
  void finalizeRiskStep_populatesLogLines() {
    AgentProcessingStep step = RiskAssessmentService.startRiskStep();
    RiskScoreResult result = new RiskScoreResult();
    result.setRiskScore(SupplierRiskScoreBuilder.of(80, 90, 70));

    RiskAssessmentService.finalizeRiskStep(step, result);

    assertThat(step.getLogLines()).isNotEmpty();
    assertThat(step.getLogLines().stream()
        .anyMatch(l -> l.getMessage().contains("Financial stability"))).isTrue();
  }
}
