package com.axonivy.utils.smart.workflow.demo.supplier.onboarding.helper;

import java.util.ArrayList;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;

import com.axonivy.utils.smart.workflow.demo.supplier.agent.SupplierAgentResponse;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.agent.AgentProcessingStep;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.enums.AgentStepStatus;

public final class AgentStepHelper {

  private AgentStepHelper() {}

  public static AgentProcessingStep createPendingStep(String stepKey, String displayName) {
    AgentProcessingStep step = new AgentProcessingStep();
    step.setStepKey(stepKey);
    step.setName(displayName);
    step.setStatus(AgentStepStatus.PENDING);
    return step;
  }

  public static void finalizeStep(AgentProcessingStep placeholder, AgentProcessingStep source) {
    if (placeholder == null) {
      return;
    }
    if (placeholder.getLogLines() == null) {
      placeholder.setLogLines(new ArrayList<>());
    }
    if (source != null) {
      placeholder.setStatus(source.getStatus());
      placeholder.setStartedAt(source.getStartedAt());
      placeholder.setCompletedAt(source.getCompletedAt());
      placeholder.setDurationMs(source.getDurationMs());
      if (source.getLogLines() != null) {
        placeholder.getLogLines().addAll(source.getLogLines());
      }
    } else {
      placeholder.setStatus(AgentStepStatus.COMPLETED);
    }
  }

  public static AgentProcessingStep getStep(int index, SupplierAgentResponse agentResponse) {
    if (agentResponse == null || agentResponse.getProcessingSteps() == null
        || agentResponse.getProcessingSteps().size() <= index) {
      return createPendingStep("STEP_" + (index + 1), "Step " + (index + 1));
    }
    return agentResponse.getProcessingSteps().get(index);
  }

  public static void addStepErrorMessage(String stepKey, Exception e,
      List<AgentProcessingStep> steps, FacesContext fc) {
    List<AgentProcessingStep> stepList = steps != null ? steps : new ArrayList<>();
    String displayName = stepList.stream()
        .filter(s -> stepKey.equals(s.getStepKey()))
        .map(AgentProcessingStep::getName)
        .findFirst()
        .orElse(stepKey);
    String detail = e.getMessage() != null ? e.getMessage() : "An unexpected error occurred.";
    fc.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, displayName + " failed", detail));
  }

}
