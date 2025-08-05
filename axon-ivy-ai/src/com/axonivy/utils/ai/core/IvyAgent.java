package com.axonivy.utils.ai.core;

import java.util.List;
import java.util.Optional;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.ai.agent.Planner;
import com.axonivy.utils.ai.connector.AbstractAiServiceConnector;
import com.axonivy.utils.ai.connector.OpenAiServiceConnector;
import com.axonivy.utils.ai.core.tool.IvyTool;
import com.axonivy.utils.ai.dto.ObservationInput;
import com.axonivy.utils.ai.dto.PlanInput;
import com.axonivy.utils.ai.dto.ai.AiVariable;
import com.axonivy.utils.ai.enums.ExecutionStatus;
import com.axonivy.utils.ai.enums.log.LogLevel;
import com.axonivy.utils.ai.enums.log.LogPhase;
import com.axonivy.utils.ai.enums.model.OpenAiModelType;
import com.axonivy.utils.ai.mapper.IvyToolMapper;
import com.axonivy.utils.ai.persistence.converter.BusinessEntityConverter;

import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.process.model.element.event.start.CallSubStart;
import dev.langchain4j.service.AiServices;

public class IvyAgent extends BaseAgent {

  private List<AiVariable> missingInputs; // Missing inputs

  public IvyAgent() {
    super();
  }

  /**
   * Temporary AI connector for planning
   * 
   */
  public AbstractAiServiceConnector getPlanningConnector() {
    OpenAiServiceConnector connector = new OpenAiServiceConnector();
    connector.init(OpenAiModelType.GPT_4O.getName());
    return connector;
  }

  /**
   * Temporary AI connector for execution
   * 
   */
  public AbstractAiServiceConnector getExecutionConnector() {
    OpenAiServiceConnector connector = new OpenAiServiceConnector();
    connector.init(OpenAiModelType.GPT_4O.getName());
    return connector;
  }

  /**
   * Starts the managing agent with a user query. Initializes variables, workers,
   * creates a plan, and executes steps with adaptive ReAct reasoning.
   * 
   * @param query The user input query.
   */
  @Override
  public ExecutionStatus start(AgentExecution execution) {

    // Generate plan
    createPlanAndUpdateExecution(execution);

    // Execute the plan
    execute(execution);

    if (CollectionUtils.isNotEmpty(missingInputs)) {
      return ExecutionStatus.PENDING;
    }
    return ExecutionStatus.DONE;
  }

  /**
   * Executes the assigned plan with adaptive ReAct reasoning between steps.
   */
  @SuppressWarnings("restriction")
  @Override
  public void execute(AgentExecution execution) {
    int runningStepNo = 1;
    boolean inProgress = true;
    int iterationCount = 0;

    execution.setRunningStep(execution.getSteps().get(0));

    if (execution.getMaxIteration() != null && execution.getMaxIteration() > 5) {
      maxIterations = execution.getMaxIteration();
    }

    if (executionModel == null) {
      executionModel = new OpenAiServiceConnector();
      executionModel.init(OpenAiModelType.GPT_4O_MINI.getName());
    }

    while (inProgress && iterationCount < maxIterations) {
      iterationCount++;
      AiStep runningStep = execution.getRunningStep();
      if (runningStep == null) {
        Ivy.log().info("No step found for stepNo: " + runningStepNo);
        break;
      }

      // Execute the step using execution connector
      List<AiVariable> aiResults = runningStep.run(execution.getVariables(), executionModel, execution.getLogger(),
          iterationCount);

      // Handle missing parameters when running a step
      if (CollectionUtils.isNotEmpty(runningStep.getTool().getMissingParameters())) {
        this.missingInputs = runningStep.getTool().getMissingParameters();
        Ivy.log().info(
            String.format("Missing parameters when running step %d, tool %s", runningStepNo,
                runningStep.getToolSignature()));
        return;
      }

      // Update step results into current variable list
      if (aiResults != null) {
        for (AiVariable stepResult : aiResults) {
          // If the variable list has variable with same name, update value of the
          // existing variable
          Optional<AiVariable> matchedVariable = execution.getVariables().stream()
              .filter(current -> current.getParameter().getDefinition().getName()
                  .equals(stepResult.getParameter().getDefinition().getName()))
              .findFirst();
          if (matchedVariable.isPresent()) {
            matchedVariable.map(AiVariable::getParameter).get().setValue(stepResult.getParameter().getValue());
            continue;
          }

          // Otherwise add the variable to the variable list
          execution.getVariables().add(stepResult);
        }
      }

      // Perform ReAct-style observation and reasoning

      // Parse result of latest step to a meaningful string to help AI understand the
      // context better
      String latestResult = parseResultOfStep(runningStep, aiResults);

      // Perform observation process
      ReActDecision decision = performReActReasoning(latestResult, runningStep, execution, iterationCount);

      // Add observation to history
      execution.getObservationHistory()
          .add(String.format("Step %d: %s -> %s", runningStep.getStepNo(), runningStep.getToolSignature(),
              latestResult));

      // Act on the ReAct decision
      if (decision.isComplete()) {
        // Goal achieved, finish execution
        inProgress = false;
        Ivy.log().info("ReAct reasoning determined goal is achieved: " + decision.getReasoning());
        execution.getLogger().logAdaptivePlan(LogPhase.COMPLETE,
            "Execution completed by ReAct reasoning:" + System.lineSeparator() + decision.toPrettyString(),
            decision.getDecisionContext(), runningStepNo, iterationCount, runningStep.getToolSignature());

        Ivy.log().info("Final result");
        Ivy.log().info(BusinessEntityConverter.entityToJsonValue(getResults()));

      } else {
        // Update plan based on reasoning
        adaptPlanBasedOnReasoning(decision, runningStep, execution, iterationCount);

      }
    }

    if (iterationCount >= maxIterations) {
      Ivy.log().info("Maximum iterations (" + maxIterations + ") reached in adaptive execution");
      execution.getLogger().log(LogLevel.STEP, LogPhase.ERROR, "Maximum iterations (" + maxIterations + ") reached",
          StringUtils.EMPTY, iterationCount);
    }
  }

  /**
   * Use tool's AI result string. If there is not AI result string, use the JSON
   * presentation of result variables instead
   * 
   * @param step
   * @param aiResults
   * @return parsed result of the target step
   */
  private String parseResultOfStep(AiStep step, List<AiVariable> aiResults) {
    String latestResult = "No result";
    if (StringUtils.isNotBlank(step.getTool().getAiResult())) {
      latestResult = step.getTool().getAiResult();
    } else if (aiResults != null) {
      latestResult = BusinessEntityConverter.entityToJsonValue(aiResults);
    }
    return latestResult;
  }

  /**
   * Performs ReAct-style reasoning about the latest step result with execution instructions
   */
  private ReActDecision performReActReasoning(String latestResult, AiStep currentStep, AgentExecution execution,
      int iterationCount) {
    // Reasoning to evaluate the result of last step
    ObservationInput observationInput = new ObservationInput();
    observationInput.setLatestResult(latestResult);
    observationInput.setCurrentStep(currentStep);
    observationInput.setExecution(execution);
    observationInput.setGoal(execution.getGoal());

    var planningModel = new OpenAiServiceConnector().buildJsonOpenAiModel().build();
    Planner observer = AiServices.builder(Planner.class).chatModel(planningModel).build();

    // Parse the response
    String decisionContext = observationInput.toString();

    try {
      ReActDecision decision = observer.makeDecision(decisionContext);
      decision.setDecisionContext(decisionContext);
      return decision;

    } catch (Exception e) {
      execution.getLogger().log(LogLevel.PLANNING, LogPhase.ERROR, "Error in ReAct reasoning: " + e.getMessage(),
          StringUtils.EMPTY, iterationCount);
      // Default to continuing with original plan
      return new ReActDecision(false, false, "Error in reasoning, continuing with plan", StringUtils.EMPTY,
          StringUtils.EMPTY, decisionContext);
    }
  }

  /**
   * Adapts the execution plan based on ReAct reasoning
   */
  @SuppressWarnings("restriction")
  private void adaptPlanBasedOnReasoning(ReActDecision decision, AiStep currentStep, AgentExecution execution,
      int iterationCount) {
    try {
      // Find the agent specified in the decision
      String targetToolSignature = decision.getSelectedSignature().trim();
      var targetTool = execution.getTools().stream()
          .filter(t -> t.getSignature().toSignatureString().contains(targetToolSignature)).findFirst()
          .orElse(null);

      if (targetTool != null) {
        // Create a new adaptive step
        AiStep adaptiveStep = createAdaptiveStep(decision, currentStep, targetTool, targetToolSignature, execution);
        execution.getSteps().clear();
        execution.getSteps().add(adaptiveStep);
        execution.setRunningStep(adaptiveStep);

        Ivy.log().info("Created new adaptive step " + adaptiveStep.getStepNo() + " with tool: " + targetToolSignature);

        execution.getLogger().logAdaptivePlan(LogPhase.RUNNING,
            "Plan adapted by ReAct reasoning:" + System.lineSeparator() + decision.toPrettyString(),
            decision.getDecisionContext(), adaptiveStep.getStepNo(), iterationCount, adaptiveStep.getToolSignature());
      } else {
        Ivy.log().info("Could not find tool for adaptive step: " + targetToolSignature);
      }
    } catch (Exception e) {
      Ivy.log().info("Error adapting plan: " + e.getMessage());
    }
  }

  /**
   * Creates a new adaptive step
   */
  @SuppressWarnings("restriction")
  private AiStep createAdaptiveStep(ReActDecision decision, AiStep currentStep, CallSubStart targetTool,
      String targetToolId, AgentExecution execution) {
    AiStep adaptiveStep = new AiStep();
    adaptiveStep.setStepNo(execution.getSteps().size() + 1);
    adaptiveStep.useTool(IvyToolMapper.fromCallSubStart.apply(targetTool));
    adaptiveStep.getTool().setConnector(getExecutionConnector());
    return adaptiveStep;
  }

  @SuppressWarnings("restriction")
  private void createPlanAndUpdateExecution(AgentExecution execution) {
    var planningModel = new OpenAiServiceConnector().buildJsonOpenAiModel().build();

    PlanInput planInput = new PlanInput();
    planInput.setQuery(execution.getParsedOriginalInputQuery());
    planInput.setInstructions(execution.getPlanningInstructions());
    planInput.setTools(execution.getTools());

    Planner planner = AiServices.builder(Planner.class).chatModel(planningModel).build();

    execution.setSteps(planner.createPlan(planInput.toString()));

    for (var step : execution.getSteps()) {
      for (var tool : execution.getTools()) {
        if (step.getToolSignature().equals(tool.getSignature().toSignatureString())) {
          IvyTool ivyTool = IvyToolMapper.fromCallSubStart.apply(tool);
          ivyTool.setConnector(getExecutionConnector());
          step.setTool(ivyTool);
          break;
        }
      }
    }
  }
}
