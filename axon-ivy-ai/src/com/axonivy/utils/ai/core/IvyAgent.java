package com.axonivy.utils.ai.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.axonivy.utils.ai.dto.ai.AiVariable;
import com.axonivy.utils.ai.dto.ai.FieldExplanation;
import com.axonivy.utils.ai.enums.InstructionType;
import com.axonivy.utils.ai.function.DataMapping;
import com.axonivy.utils.ai.function.Planning;
import com.axonivy.utils.ai.persistence.converter.BusinessEntityConverter;

import dev.langchain4j.model.input.PromptTemplate;

public class IvyAgent extends BaseAgent {

  // IvyAgent-specific field
  protected String goal;                           // Agent's primary objective (required for IvyAgent)

  private static final String EXECUTION_PROMPT_TEMPLATE = """
      GOAL: {{goal}}

      {{executionInstructions}}CURRENT SITUATION:
      Original Query: {{originalQuery}}
      Current Step: {{currentStepName}}
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
      Agent Selection: [Which tool ID should handle the next step]
      Reasoning: [Why this change is needed]

      If Decision is COMPLETE, provide:
      Final Reasoning: [Why the goal has been achieved]
      """;

  // Ordered list of steps that define the agent's behavior or process
  private List<AiStep> steps;

  public IvyAgent() {
    super();
  }

  /**
   * Override to handle IvyAgent-specific goal field loading
   */
  @Override
  public void loadFromModel(com.axonivy.utils.ai.dto.ai.configuration.AgentModel model) {
    super.loadFromModel(model);
    this.goal = model.getGoal(); // IvyAgent requires goal field
  }

  /**
   * Builds an enhanced planning prompt that includes goal and planning instructions
   */
  private String buildPlanningPrompt(String query) {
    StringBuilder prompt = new StringBuilder();
    
    if (goal != null && !goal.isEmpty()) {
      prompt.append("GOAL: ").append(goal).append(TWO_LINES);
    }

    prompt.append("USER QUERY: ").append(query).append(TWO_LINES);

    // Add planning instructions if available
    List<String> planningInstructions = getInstructionsByType(InstructionType.PLANNING);
    if (!planningInstructions.isEmpty()) {
      prompt.append("PLANNING INSTRUCTIONS:").append(ONE_LINE);
      for (int i = 0; i < planningInstructions.size(); i++) {
        prompt.append((i + 1)).append(". ").append(planningInstructions.get(i)).append(ONE_LINE);
      }
      prompt.append(ONE_LINE);
    }

    prompt.append("Available tools: ");
    prompt.append(tools.stream().map(tool -> tool.getId()).collect(java.util.stream.Collectors.joining(", ")));
    prompt.append(TWO_LINES).append("Create a detailed execution plan to achieve the goal:");
    return prompt.toString();
  }

  /**
   * Starts the managing agent with a user query. Initializes variables, workers,
   * creates a plan, and executes steps with adaptive ReAct reasoning.
   * 
   * @param query The user input query.
   */
  @Override
  public void start(String query) {
    this.originalQuery = query;
    this.observationHistory = new ArrayList<>();

    // Prepare input variable
    setVariables(new ArrayList<>());
    AiVariable inputVariable = new AiVariable();
    inputVariable.init();
    inputVariable.setName("query");
    inputVariable.setContent(query);
    inputVariable.setDescription("The input query");
    getVariables().add(inputVariable);

    // Generate high-level plan from query using the planning AI model
    String enhancedPlanningPrompt = buildPlanningPrompt(query);
    Planning.Builder planningBuilder = Planning.getBuilder()
        .addTools(tools)
        .useService(planningModel)
        .withQuery(enhancedPlanningPrompt);
    
    // Add planning instructions as custom instructions
    List<String> planningInstructions = getInstructionsByType(InstructionType.PLANNING);
    for (String instruction : planningInstructions) {
      planningBuilder.addCustomInstruction(instruction);
    }
    
    Planning planning = planningBuilder.build();
    String crudePlan = planning.execute().getContent();

    // Map plan content to a list of AiSteps using execution AI model
    String stepString = DataMapping.getBuilder().useService(executionModel).withObject(new AiStep())
        .addFieldExplanations(Arrays.asList(new FieldExplanation("stepNo", "Incremental integer, starts at 1"),
            new FieldExplanation("name", "Name of the step"), new FieldExplanation("analysis", "Analysis of the step"),
            new FieldExplanation("toolId", "Tool ID to execute the step"),
            new FieldExplanation("next", "ID of the next step, -1 if final"),
            new FieldExplanation("previous", "ID of the previous step, 0 if initial"),
            new FieldExplanation("resultName", "Expected result name"),
            new FieldExplanation("resultDescription", "Expected result description")))
        .withQuery(crudePlan).asList(true).build().execute().getContent();

    List<AiStep> plannedSteps = BusinessEntityConverter.jsonValueToEntities(stepString, AiStep.class);

    // Assign each step to a corresponding worker agent by runnerId
    steps = new ArrayList<>();
    for (AiStep step : plannedSteps) {

      var tool = tools.stream().filter(w -> w.getId().equals(step.getToolId())).findFirst()
          .orElseThrow(() -> new IllegalArgumentException("Tool not found for ID: " + step.getToolId()));
      step.useTool(tool);
      steps.add(step);
    }

    execute();
  }

  /**
   * Executes the assigned plan with adaptive ReAct reasoning between steps.
   */
  @Override
  public void execute() {
    if (getVariables() == null) {
      setVariables(new ArrayList<>());
    }

    historyLog = new com.axonivy.utils.ai.history.HistoryLog();

    // Log input variables with goal information
    String inputVariablesStr = "Start running adaptive managing agent";
    if (goal != null && !goal.isEmpty()) {
      inputVariablesStr += " with goal: " + goal;
    }
    if (!getVariables().isEmpty()) {
      inputVariablesStr += "\nInputs\n-----------------\n" + BusinessEntityConverter.entityToJsonValue(getVariables());
    }
    historyLog.addSystemMessage(inputVariablesStr, AiStep.INITIAL_STEP);

    int runningStepNo = AiStep.INITIAL_STEP;
    boolean inProgress = true;
    int iterationCount = 0;

    while (inProgress && iterationCount < maxIterations) {
      iterationCount++;
      AiStep runningStep = getStepByNumber(runningStepNo);
      if (runningStep == null) {
        System.err.println("No step found for stepNo: " + runningStepNo);
        break;
      }

      // Execute the step using execution connector
      List<AiVariable> aiResults = runningStep.run(getVariables(), executionModel);

      // Add step result into current variable list
      if (aiResults != null) {
        getVariables().addAll(aiResults);

        // Log step result
        historyLog.addSystemMessage(BusinessEntityConverter.entityToJsonValue(aiResults), runningStep.getStepNo());
      }

      // Perform ReAct-style observation and reasoning
      String latestResult = aiResults != null ? BusinessEntityConverter.entityToJsonValue(aiResults) : "No result";

      ReActDecision decision = performReActReasoning(latestResult, runningStep);

      // Add observation to history
      observationHistory
          .add(String.format("Step %d: %s -> %s", runningStep.getStepNo(), runningStep.getName(), latestResult));

      // Act on the ReAct decision
      if (decision.isComplete()) {
        // Goal achieved, finish execution
        inProgress = false;
        System.err.println("ReAct reasoning determined goal is achieved: " + decision.getReasoning());
        historyLog.addSystemMessage("Execution completed by ReAct reasoning: " + decision.getReasoning(),
            runningStep.getStepNo());
      } else if (decision.shouldUpdatePlan()) {
        // Update plan based on reasoning
        runningStepNo = adaptPlanBasedOnReasoning(decision, runningStep);
        historyLog.addSystemMessage("Plan adapted by ReAct reasoning: " + decision.getReasoning(),
            runningStep.getStepNo());
      } else {
        // Continue with original plan
        runningStepNo = runningStep.getNext();
      }

      // Final step logic
      if (runningStepNo == AiStep.FINALIZE_STEP) {
        inProgress = false;
        System.err.println("managing final result");
        System.err.println(BusinessEntityConverter.entityToJsonValue(getResults()));
      }
    }

    if (iterationCount >= maxIterations) {
      System.err.println("Maximum iterations (" + maxIterations + ") reached in adaptive execution");
      historyLog.addSystemMessage("Maximum iterations (" + maxIterations + ") reached", iterationCount);
    }
  }

  /**
   * Performs ReAct-style reasoning about the latest step result with execution instructions
   */
  private ReActDecision performReActReasoning(String latestResult, AiStep currentStep) {
    try {
      // Build enhanced reasoning prompt with execution instructions
      String enhancedReasoningPrompt = buildExecutionReasoningPrompt(latestResult, currentStep);
      
      // Get AI reasoning using execution connector
      String aiResponse = executionModel.generate(enhancedReasoningPrompt);

      // Parse the response
      return ReActDecision.parseReActDecision(aiResponse);

    } catch (Exception e) {
      System.err.println("Error in ReAct reasoning: " + e.getMessage());
      historyLog.addSystemMessage("Error in ReAct reasoning: " + e.getMessage(), currentStep.getStepNo());
      // Default to continuing with original plan
      return new ReActDecision(false, false, "Error in reasoning, continuing with plan", "", "");
    }
  }

  /**
   * Builds enhanced reasoning prompt with execution instructions
   */
  private String buildExecutionReasoningPrompt(String latestResult, AiStep currentStep) {
    // Prepare execution instructions section
    List<String> executionInstructions = getInstructionsByType(InstructionType.EXECUTION);
    String executionInstructionsText = "";
    if (!executionInstructions.isEmpty()) {
      StringBuilder instructionsBuilder = new StringBuilder();
      instructionsBuilder.append("EXECUTION INSTRUCTIONS:\n");
      for (int i = 0; i < executionInstructions.size(); i++) {
        instructionsBuilder.append((i + 1)).append(". ").append(executionInstructions.get(i)).append(ONE_LINE);
      }
      instructionsBuilder.append(ONE_LINE);
      executionInstructionsText = instructionsBuilder.toString();
    }

    // Build template parameters
    java.util.Map<String, Object> params = new java.util.HashMap<>();
    params.put("goal", goal != null ? goal : "Complete the user request");
    params.put("executionInstructions", executionInstructionsText);
    params.put("originalQuery", originalQuery);
    params.put("currentStepName", currentStep.getName());
    params.put("latestResult", latestResult);
    params.put("observationHistory", String.join("\n", observationHistory));
    params.put("availableTools", buildAvailableToolsDescription());

    return PromptTemplate.from(EXECUTION_PROMPT_TEMPLATE).apply(params).text();
  }

  /**
   * Adapts the execution plan based on ReAct reasoning
   */
  private int adaptPlanBasedOnReasoning(ReActDecision decision, AiStep currentStep) {
    try {
      // Find the agent specified in the decision
      String targetToolId = decision.getAgentSelection().trim();
      var targetTool = tools.stream().filter(t -> t.getId().equals(targetToolId)).findFirst().orElse(null);

      if (targetTool != null) {
        // Create a new adaptive step
        AiStep adaptiveStep = new AiStep();
        adaptiveStep.setStepNo(currentStep.getStepNo() + 1000); // Use high number to avoid conflicts
        adaptiveStep.setName("Adaptive: " + decision.getNextAction());
        adaptiveStep.setAnalysis("Dynamically created based on ReAct reasoning");
        adaptiveStep.setPrevious(currentStep.getStepNo());
        adaptiveStep.setNext(AiStep.FINALIZE_STEP); // Default to final, can be changed by further reasoning
        adaptiveStep.useTool(targetTool);
        adaptiveStep.setToolId(targetToolId);

        // Add the adaptive step to our steps list
        steps.add(adaptiveStep);

        return adaptiveStep.getStepNo();
      } else {
        System.err.println("Could not find tool for adaptive step: " + targetToolId);
        return currentStep.getNext(); // Fall back to original plan
      }

    } catch (Exception e) {
      System.err.println("Error adapting plan: " + e.getMessage());
      return currentStep.getNext(); // Fall back to original plan
    }
  }

  /**
   * Retrieves the step by its number.
   *
   * @param stepNo the step number to find
   * @return the matching AiStep or null
   */
  private AiStep getStepByNumber(int stepNo) {
    return steps.stream().filter(
        step -> stepNo == AiStep.INITIAL_STEP ? step.getPrevious() == AiStep.INITIAL_STEP : step.getStepNo() == stepNo)
        .findFirst().orElse(null);
  }

  public List<AiStep> getSteps() {
    return steps;
  }

  public void setSteps(List<AiStep> steps) {
    this.steps = steps;
  }

  public String getGoal() {
    return goal;
  }

  public void setGoal(String goal) {
    this.goal = goal;
  }
}
