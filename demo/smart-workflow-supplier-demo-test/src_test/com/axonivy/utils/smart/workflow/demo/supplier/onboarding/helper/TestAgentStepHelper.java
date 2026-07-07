package com.axonivy.utils.smart.workflow.demo.supplier.onboarding.helper;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.axonivy.utils.smart.workflow.demo.supplier.agent.SupplierAgentResponse;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.agent.AgentProcessingStep;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.enums.AgentStepStatus;

import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
class TestAgentStepHelper {

  @Test
  void createPendingStep_setsKeyNameAndStatus() {
    AgentProcessingStep step = AgentStepHelper.createPendingStep("STEP_1", "Document Extraction");

    assertThat(step.getStepKey()).isEqualTo("STEP_1");
    assertThat(step.getName()).isEqualTo("Document Extraction");
    assertThat(step.getStatus()).isEqualTo(AgentStepStatus.PENDING);
  }

  @Test
  void finalizeStep_handlesNullArguments() {
    AgentProcessingStep step = AgentStepHelper.createPendingStep("STEP_1", "Step 1");

    AgentStepHelper.finalizeStep(step, null);
    assertThat(step.getStatus()).isEqualTo(AgentStepStatus.COMPLETED);
    assertThat(step.getLogLines()).isNotNull().isEmpty();

    AgentStepHelper.finalizeStep(null, step);
  }

  @Test
  void finalizeStep_whenSourceIsValid_copiesAllFields() {
    AgentProcessingStep placeholder = AgentStepHelper.createPendingStep("STEP_1", "Step 1");

    AgentProcessingStep source = new AgentProcessingStep();
    source.setStatus(AgentStepStatus.COMPLETED);
    source.setStartedAt(1000L);
    source.setCompletedAt(3000L);
    source.setDurationMs(2000L);

    AgentStepHelper.finalizeStep(placeholder, source);

    assertThat(placeholder.getStatus()).isEqualTo(AgentStepStatus.COMPLETED);
    assertThat(placeholder.getStartedAt()).isEqualTo(1000L);
    assertThat(placeholder.getCompletedAt()).isEqualTo(3000L);
    assertThat(placeholder.getDurationMs()).isEqualTo(2000L);
  }

  @Test
  void getStep_whenIndexInBounds_returnsStep() {
    SupplierAgentResponse response = new SupplierAgentResponse();
    AgentProcessingStep step = AgentStepHelper.createPendingStep("STEP_1", "Step 1");
    response.setProcessingSteps(new ArrayList<>(List.of(step)));

    AgentProcessingStep result = AgentStepHelper.getStep(0, response);

    assertThat(result).isSameAs(step);
  }

  @Test
  void getStep_whenIndexOutOfBounds_returnsPendingPlaceholder() {
    SupplierAgentResponse response = new SupplierAgentResponse();
    response.setProcessingSteps(new ArrayList<>());

    AgentProcessingStep result = AgentStepHelper.getStep(2, response);

    assertThat(result).isNotNull();
    assertThat(result.getStatus()).isEqualTo(AgentStepStatus.PENDING);
    assertThat(result.getStepKey()).isEqualTo("STEP_3");
  }

  @Test
  void getStep_returnsPendingPlaceholder_whenNoStepsAvailable() {
    assertThat(AgentStepHelper.getStep(0, null).getStatus()).isEqualTo(AgentStepStatus.PENDING);
    assertThat(AgentStepHelper.getStep(0, new SupplierAgentResponse()).getStatus()).isEqualTo(AgentStepStatus.PENDING);
  }
}
