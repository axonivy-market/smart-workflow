package com.axonivy.utils.smart.workflow.demo.supplier.onboarding.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.axonivy.utils.smart.workflow.demo.supplier.agent.PolicyValidationResult;
import com.axonivy.utils.smart.workflow.demo.supplier.agent.SupplierAgentResponse;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.ValidationFinding;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.enums.AgentStepStatus;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.enums.FindingSeverity;

import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
class TestAgentAnalysisService {

  @Test
  void startAnalysis_creates4PendingStepsWithNames() {
    SupplierAgentResponse response = new AgentAnalysisService().startAnalysis(
        "Step 1", "Step 2", "Step 3", "Step 4");

    assertThat(response.getProcessingSteps()).hasSize(4);
    assertThat(response.getProcessingSteps())
        .allMatch(s -> AgentStepStatus.PENDING.equals(s.getStatus()));
    assertThat(response.getProcessingSteps().get(0).getName()).isEqualTo("Step 1");
    assertThat(response.getProcessingSteps().get(1).getName()).isEqualTo("Step 2");
    assertThat(response.getProcessingSteps().get(2).getName()).isEqualTo("Step 3");
    assertThat(response.getProcessingSteps().get(3).getName()).isEqualTo("Step 4");
  }

  @Test
  void mergeFindings_filtersOutPassedFindings() throws Exception {
    PolicyValidationResult policy = new PolicyValidationResult();
    policy.setFindings(new ArrayList<>(List.of(
        finding("Missing ISO cert", FindingSeverity.FAILURE),
        finding("All good", FindingSeverity.PASSED))));

    List<ValidationFinding> result = invokemergeFindings(policy, null);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getMessage()).isEqualTo("Missing ISO cert");
  }

  @Test
  void mergeFindings_deduplicatesByMessage() throws Exception {
    PolicyValidationResult policy = new PolicyValidationResult();
    policy.setFindings(new ArrayList<>(List.of(
        finding("Duplicate message", FindingSeverity.FAILURE))));

    PolicyValidationResult financial = new PolicyValidationResult();
    financial.setFindings(new ArrayList<>(List.of(
        finding("Duplicate message", FindingSeverity.WARNING),
        finding("Unique finding", FindingSeverity.WARNING))));

    List<ValidationFinding> result = invokemergeFindings(policy, financial);

    assertThat(result).hasSize(2);
    assertThat(result.stream().map(ValidationFinding::getMessage))
        .containsExactly("Duplicate message", "Unique finding");
  }

  @Test
  void mergeFindings_returnsEmpty_whenNoValidFindings() throws Exception {
    assertThat(invokemergeFindings(null, null)).isEmpty();

    PolicyValidationResult policy = new PolicyValidationResult();
    assertThat(invokemergeFindings(policy, null)).isEmpty();
  }

  @SuppressWarnings("unchecked")
  private static List<ValidationFinding> invokemergeFindings(
      PolicyValidationResult policy, PolicyValidationResult financial) throws Exception {
    Method m = AgentAnalysisService.class.getDeclaredMethod(
        "mergeFindings", PolicyValidationResult.class, PolicyValidationResult.class);
    m.setAccessible(true);
    return (List<ValidationFinding>) m.invoke(new AgentAnalysisService(), policy, financial);
  }

  private static ValidationFinding finding(String message, FindingSeverity severity) {
    ValidationFinding f = new ValidationFinding();
    f.setMessage(message);
    f.setSeverity(severity);
    return f;
  }
}
