package com.axonivy.utils.ai.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.ai.connector.AbstractAiServiceConnector;
import com.axonivy.utils.ai.connector.OpenAiServiceConnector;
import com.axonivy.utils.ai.core.tool.IvyTool;
import com.axonivy.utils.ai.dto.ai.AiVariable;
import com.axonivy.utils.ai.dto.ai.FieldExplanation;
import com.axonivy.utils.ai.dto.ai.configuration.GoalBasedAgentModel;
import com.axonivy.utils.ai.function.DataMapping;
import com.axonivy.utils.ai.function.Planning;
import com.axonivy.utils.ai.history.HistoryLog;
import com.axonivy.utils.ai.persistence.converter.BusinessEntityConverter;
import com.fasterxml.jackson.annotation.JsonIgnore;

import ch.ivyteam.ivy.environment.Ivy;
import dev.langchain4j.model.input.PromptTemplate;

public class IvyAgent {

  private static final int MAX_ITERATIONS = 20; // Prevent infinite loops

  // ReAct reasoning template for plan adaptation
  private static final String REACT_REASONING_TEMPLATE = """
      Original Query: {{originalQuery}}

      Current Plan Status:
      {{planStatus}}

      Latest Step Result:
      {{latestResult}}

      Observation History:
      {{observationHistory}}

      Available Tools:
      {{availableTools}}

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

  // Pattern to parse ReAct reasoning response
  private static final Pattern DECISION_PATTERN = Pattern.compile("Decision:\\s*(YES|NO|COMPLETE)",
      Pattern.CASE_INSENSITIVE);
  private static final Pattern NEXT_ACTION_PATTERN = Pattern.compile("Next Action:\\s*(.+?)(?=\\n|$)", Pattern.DOTALL);
  private static final Pattern AGENT_SELECTION_PATTERN = Pattern.compile("Agent Selection:\\s*(.+?)(?=\\n|$)",
      Pattern.DOTALL);

  // Unique identifier for the agent
  private String id;

  // Ordered list of steps that define the agent's behavior or process
  private List<AiStep> steps;

  // Human-readable name for the agent
  private String name;

  // Description or intended usage of the agent
  private String usage;

  // List of variables used or produced by the agent
  private List<AiVariable> variables;

  // Execution history log (not serialized to JSON)
  @JsonIgnore
  private HistoryLog historyLog;

  private AbstractAiServiceConnector connector;

  private List<String> observationHistory;

  private String originalQuery;

  private List<IvyTool> tools;

  // List of results collected during agent execution (not serialized to JSON)
  @JsonIgnore
  private List<AiVariable> results;

  public IvyAgent() {
    id = UUID.randomUUID().toString().replaceAll("-", StringUtils.EMPTY);
  }

  /**
   * Load the agent from model.
   */
  public void loadFromModel(GoalBasedAgentModel model) {
    this.id = model.getId();
    this.name = model.getName();
    this.usage = model.getUsage();

    this.connector = OpenAiServiceConnector.getTinyBrain();

    this.tools = new ArrayList<>();
    List<IvyTool> foundTools = BusinessEntityConverter.jsonValueToEntities(Ivy.var().get("Ai.Tools"), IvyTool.class);
    for (String toolName : model.getTools()) {
      Optional<IvyTool> ivyTool = foundTools.stream().filter(t -> t.getId().equals(toolName)).findFirst();
      if (ivyTool.isPresent()) {
        this.tools.add(ivyTool.get());
      }
    }
  }

  /**
   * Starts the managing agent with a user query. Initializes variables, workers,
   * creates a plan, and executes steps with adaptive ReAct reasoning.
   * 
   * @param query The user input query.
   */
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

    // Generate high-level plan from query using a large AI model
    Planning planning = Planning.getBuilder().addTools(tools).useService(connector).withQuery(query).build();
    String crudePlan = planning.execute().getContent();

    // Map plan content to a list of AiSteps using a smaller AI model
    String stepString = DataMapping.getBuilder().useService(connector).withTargetObject(new AiStep())
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

      IvyTool tool = tools.stream().filter(w -> w.getId().equals(step.getToolId())).findFirst()
          .orElseThrow(() -> new IllegalArgumentException("Tool not found for ID: " + step.getToolId()));
      step.useTool(tool);
      steps.add(step);
    }

    execute();
  }

  /**
   * Executes the assigned plan with adaptive ReAct reasoning between steps.
   */
  public void execute() {
    if (getVariables() == null) {
      setVariables(new ArrayList<>());
    }

    historyLog = new HistoryLog();

    // Log input variables
    String inputVariablesStr = "Start running adaptive managing agent";
    if (!getVariables().isEmpty()) {
      inputVariablesStr = "Inputs\n-----------------\n" + BusinessEntityConverter.entityToJsonValue(getVariables());
    }
    historyLog.addSystemMessage(inputVariablesStr, AiStep.INITIAL_STEP);

    int runningStepNo = AiStep.INITIAL_STEP;
    boolean inProgress = true;
    int iterationCount = 0;

    while (inProgress && iterationCount < MAX_ITERATIONS) {
      iterationCount++;
      AiStep runningStep = getStepByNumber(runningStepNo);
      if (runningStep == null) {
        System.err.println("No step found for stepNo: " + runningStepNo);
        break;
      }

      // Execute the step
      List<AiVariable> aiResults = runningStep.run(getVariables(), connector);

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

    if (iterationCount >= MAX_ITERATIONS) {
      System.err.println("Maximum iterations reached in adaptive execution");
      historyLog.addSystemMessage("Maximum iterations reached", iterationCount);
    }
  }

  /**
   * Performs ReAct-style reasoning about the latest step result
   */
  private ReActDecision performReActReasoning(String latestResult, AiStep currentStep) {
    try {
      // Build the reasoning prompt
      Map<String, Object> params = new HashMap<>();
      params.put("originalQuery", originalQuery);
      params.put("planStatus", buildPlanStatus(currentStep));
      params.put("latestResult", latestResult);
      params.put("observationHistory", String.join("\n", observationHistory));
      params.put("availableTools", buildAvailableToolsDescription());

      String reasoningPrompt = PromptTemplate.from(REACT_REASONING_TEMPLATE).apply(params).text();

      // Get AI reasoning
      String aiResponse = connector.generate(reasoningPrompt);

      // Parse the response
      return parseReActDecision(aiResponse);

    } catch (Exception e) {
      System.err.println("Error in ReAct reasoning: " + e.getMessage());
      // Default to continuing with original plan
      return new ReActDecision(false, false, "Error in reasoning, continuing with plan", "", "");
    }
  }

  /**
   * Adapts the execution plan based on ReAct reasoning
   */
  private int adaptPlanBasedOnReasoning(ReActDecision decision, AiStep currentStep) {
    try {
      // Find the agent specified in the decision
      String targetToolId = decision.getAgentSelection().trim();
      IvyTool targetTool = tools.stream().filter(t -> t.getId().equals(targetToolId)).findFirst().orElse(null);

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
   * Builds a description of the current plan status
   */
  private String buildPlanStatus(AiStep currentStep) {
    StringBuilder status = new StringBuilder();
    status.append("Current step: ").append(currentStep.getStepNo()).append(" - ").append(currentStep.getName())
        .append("\n");

    status.append("Remaining steps:\n");
    for (AiStep step : steps) {
      if (step.getStepNo() > currentStep.getStepNo()) {
        status.append("  Step ").append(step.getStepNo()).append(": ").append(step.getName()).append("\n");
      }
    }

    return status.toString();
  }

  /**
   * Builds a description of available worker agents
   */
  private String buildAvailableToolsDescription() {
    StringBuilder toolsStr = new StringBuilder();
    for (var tool : tools) {
      toolsStr.append("- ID: ").append(tool.getId()).append(", Name: ").append(tool.getName()).append(", Usage: ")
          .append(tool.getUsage()).append(System.lineSeparator());
    }
    return toolsStr.toString();
  }

  /**
   * Parses the AI response to extract ReAct decision
   */
  private ReActDecision parseReActDecision(String aiResponse) {
    // Parse decision
    Matcher decisionMatcher = DECISION_PATTERN.matcher(aiResponse);
    String decision = decisionMatcher.find() ? decisionMatcher.group(1).toUpperCase() : "NO";

    boolean shouldUpdate = "YES".equals(decision);
    boolean isComplete = "COMPLETE".equals(decision);

    // Parse next action if updating
    String nextAction = "";
    String agentSelection = "";

    if (shouldUpdate) {
      Matcher actionMatcher = NEXT_ACTION_PATTERN.matcher(aiResponse);
      if (actionMatcher.find()) {
        nextAction = actionMatcher.group(1).trim();
      }

      Matcher agentMatcher = AGENT_SELECTION_PATTERN.matcher(aiResponse);
      if (agentMatcher.find()) {
        agentSelection = agentMatcher.group(1).trim();
      }
    }

    return new ReActDecision(shouldUpdate, isComplete, aiResponse, nextAction, agentSelection);
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

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public List<AiStep> getSteps() {
    return steps;
  }

  public void setSteps(List<AiStep> steps) {
    this.steps = steps;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getUsage() {
    return usage;
  }

  public void setUsage(String usage) {
    this.usage = usage;
  }

  public List<AiVariable> getVariables() {
    return variables;
  }

  public void setVariables(List<AiVariable> variables) {
    this.variables = variables;
  }

  public List<AiVariable> getResults() {
    return results;
  }

  public void setResults(List<AiVariable> results) {
    this.results = results;
  }

  public AbstractAiServiceConnector getConnector() {
    return connector;
  }

  public void setConnector(AbstractAiServiceConnector connector) {
    this.connector = connector;
  }

  /**
   * Helper class to represent a ReAct decision
   */
  private static class ReActDecision {
    private final boolean shouldUpdatePlan;
    private final boolean isComplete;
    private final String reasoning;
    private final String nextAction;
    private final String agentSelection;

    public ReActDecision(boolean shouldUpdatePlan, boolean isComplete, String reasoning, String nextAction,
        String agentSelection) {
      this.shouldUpdatePlan = shouldUpdatePlan;
      this.isComplete = isComplete;
      this.reasoning = reasoning;
      this.nextAction = nextAction;
      this.agentSelection = agentSelection;
    }

    public boolean shouldUpdatePlan() {
      return shouldUpdatePlan;
    }

    public boolean isComplete() {
      return isComplete;
    }

    public String getReasoning() {
      return reasoning;
    }

    public String getNextAction() {
      return nextAction;
    }

    public String getAgentSelection() {
      return agentSelection;
    }
  }
}
