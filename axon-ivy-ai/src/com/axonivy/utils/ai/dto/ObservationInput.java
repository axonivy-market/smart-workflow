package com.axonivy.utils.ai.dto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.ai.core.AgentExecution;
import com.axonivy.utils.ai.core.AiStep;
import com.axonivy.utils.ai.dto.ai.Instruction;

import dev.langchain4j.model.input.PromptTemplate;

public class ObservationInput {

  private static final String TEMPLATE = """
      GOAL: {{goal}}

      {{executionInstructions}}

      CURRENT SITUATION:
      Original Query: {{originalQuery}}
      Current Step: {{currentStepNo}}
      Latest Result: {{latestResult}}
      Observation History:
      {{observationHistory}}

      AVAILABLE TOOLS:
      {{availableTools}}

      ANALYSIS REQUIRED:
      1. Is the goal achieved? (Consider the execution instructions)
      2. If not, should we continue with the current plan or adapt it?
      3. Are there any execution instruction triggers based on the latest result?

      Based on the latest step result and current progress, analyze if the plan needs to be updated:

      Thought: Do I need to modify the execution plan based on the observation?
      Reasoning: [Analyze the result and determine if plan changes are needed]
      Decision: [YES - update plan | NO - continue with current plan | COMPLETE - goal achieved]

      If Decision is YES, provide:
      Next Action: [What should be the next step]
      Tool Signature: [Signature of the tool to handle the next step. MUST be exactly one of the provided signatures above]
      Reasoning: [Why this change is needed]

      If Decision is COMPLETE, provide:
      Final Reasoning: [Why the goal has been achieved]
      """;

  private String latestResult;
  private AiStep currentStep;
  private AgentExecution execution;
  private String goal;

  public String getLatestResult() {
    return latestResult;
  }

  public void setLatestResult(String latestResult) {
    this.latestResult = latestResult;
  }

  public AiStep getCurrentStep() {
    return currentStep;
  }

  public void setCurrentStep(AiStep currentStep) {
    this.currentStep = currentStep;
  }

  public AgentExecution getExecution() {
    return execution;
  }

  public void setExecution(AgentExecution execution) {
    this.execution = execution;
  }

  public String getGoal() {
    return goal;
  }

  public void setGoal(String goal) {
    this.goal = goal;
  }

  @Override
  public String toString() {
    return PromptTemplate.from(TEMPLATE).apply(buildParameters()).text();
  }

  private Map<String, Object> buildParameters() {
    // Prepare execution instructions section
    List<String> executionInstructions = Optional
        .ofNullable(execution.getExecutionInstructionsOf(currentStep.getToolSignature()).stream()
            .map(Instruction::getContent)
            .filter(StringUtils::isNotBlank).toList())
        .orElseGet(() -> new ArrayList<>());

    String executionInstructionsText = StringUtils.EMPTY;
    if (!executionInstructions.isEmpty()) {
      StringBuilder instructionsBuilder = new StringBuilder();
      instructionsBuilder.append("EXECUTION INSTRUCTIONS:\n");
      for (int i = 0; i < executionInstructions.size(); i++) {
        instructionsBuilder.append((i + 1)).append(". ").append(executionInstructions.get(i))
            .append(System.lineSeparator());
      }
      instructionsBuilder.append(System.lineSeparator());
      executionInstructionsText = instructionsBuilder.toString();
    }

    // Build template parameters
    Map<String, Object> params = new HashMap<>();
    params.put("goal", StringUtils.isNotBlank(goal) ? goal.strip() : "Complete the user request");
    params.put("executionInstructions", executionInstructionsText);
    params.put("originalQuery", execution.getParsedOriginalInputQuery());
    params.put("currentStepNo", Integer.toString(currentStep.getStepNo()));
    params.put("latestResult", latestResult);
    params.put("observationHistory", String.join(System.lineSeparator(), execution.getObservationHistory()));
    params.put("availableTools", buildAvailableToolsDescription());

    return params;
  }

  /**
   * Builds a description of available tools
   */
  @SuppressWarnings("restriction")
  private String buildAvailableToolsDescription() {
    StringBuilder toolsStr = new StringBuilder();
    for (var tool : execution.getTools()) {
      toolsStr.append(String.format("Tool %d", execution.getTools().indexOf(tool)))
        .append(System.lineSeparator())
          .append(String.format("  - Signature: %s", tool.getSignature().toSignatureString()))
        .append(System.lineSeparator())
        .append(String.format("  - Description: %s", tool.getDescription()))
        .append(System.lineSeparator());
    }
    return toolsStr.toString().strip();
  }
}
