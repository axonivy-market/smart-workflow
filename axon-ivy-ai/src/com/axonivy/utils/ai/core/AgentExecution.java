package com.axonivy.utils.ai.core;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.axonivy.utils.ai.core.log.ExecutionLogger;
import com.axonivy.utils.ai.dto.ai.AiVariable;
import com.axonivy.utils.ai.dto.ai.Instruction;
import com.axonivy.utils.ai.enums.ExecutionStatus;
import com.axonivy.utils.ai.enums.InstructionType;
import com.axonivy.utils.ai.persistence.converter.BusinessEntityConverter;
import com.axonivy.utils.ai.utils.IdGenerationUtils;
import com.fasterxml.jackson.annotation.JsonIgnore;

import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.process.model.element.event.start.CallSubStart;
import ch.ivyteam.ivy.process.model.value.scripting.QualifiedType;
import ch.ivyteam.ivy.process.model.value.scripting.VariableDesc;
import ch.ivyteam.ivy.process.model.value.scripting.VariableInfo;

/**
 * Represents a single execution instance of an AI agent.
 * Contains all state information needed to track and manage
 * an agent's execution lifecycle.
 */
public class AgentExecution implements Serializable {

  private static final long serialVersionUID = -7438395505679440025L;

//Execution metadata
  private String id;
  private String agentName;
  private ExecutionStatus status;
  private String username;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  // Execution state (moved from BaseAgent)
  private Object input;
  private List<AiVariable> variables;
  private List<AiVariable> results;
  private List<String> observationHistory;

  // Given tools that agent can use
  @SuppressWarnings("restriction")
  private List<CallSubStart> tools;
  private List<Instruction> instructions;

  private List<AiStep> steps;
  private int currentStepNo;
  private String plan;

  // Execution control
  private int iterationCount;
  private String errorMessage;

  @JsonIgnore
  private ExecutionLogger logger;

  @JsonIgnore
  private String parsedOriginalInputQuery;

  private String goal;

  private Integer maxIteration;

  private AiStep runningStep;

  /**
   * Constructor with basic execution info
   */
  @SuppressWarnings("restriction")
  public AgentExecution(String agentName, String username, Object input, List<CallSubStart> tools,
      List<Instruction> instructions, String goal, Integer maxIterations) {
    this.id = IdGenerationUtils.generateRandomId();
    this.agentName = agentName;
    this.input = input;
    this.username = Ivy.session().getSessionUserName();
    this.status = ExecutionStatus.PENDING;
    this.createdAt = LocalDateTime.now();
    this.updatedAt = LocalDateTime.now();
    this.variables = new ArrayList<>();
    this.results = new ArrayList<>();
    this.observationHistory = new ArrayList<>();
    this.steps = new ArrayList<>();
    this.currentStepNo = 0;
    this.iterationCount = 0;
    this.tools = tools;
    this.instructions = instructions;
    this.goal = goal;
    this.maxIteration = maxIterations;

    // Use the query to initialize variables.
    processInput();

    // Init logger
    logger = new ExecutionLogger(id);
  }

  /**
   * Processes input query and creates variables. If query is a valid JSON object
   * {"field1": "value1", "field2": "value2"}, splits JSON properties into
   * individual variables. Otherwise, creates a single "query" variable with the
   * entire input.
   * 
   * @param query The input query string
   */
  protected void processInput() {
    if (getVariables() == null) {
      setVariables(new ArrayList<>());
    }

    if (input instanceof String) {
      createSingleQueryVariable((String) input);
    } else {
      createSingleQueryVariable(BusinessEntityConverter.entityToJsonValue(input));
    }
  }

  /**
   * Creates a single query variable with the entire input
   */
  @SuppressWarnings("restriction")
  private void createSingleQueryVariable(String query) {
    AiVariable inputVariable = new AiVariable();
    inputVariable.init();
    VariableDesc variableDesc = new VariableDesc("query", new QualifiedType(String.class.getName()),
        new VariableInfo("The original input query"));
    inputVariable.getParameter().setDefinition(variableDesc);
    inputVariable.getParameter().setValue(query);
    getVariables().add(inputVariable);
    parsedOriginalInputQuery = query;
  }

  /**
   * Updates the timestamp whenever execution state changes
   */
  public void updateTimestamp() {
    this.updatedAt = LocalDateTime.now();
  }

  /**
   * Sets status and updates timestamp
   */
  public void setStatus(ExecutionStatus status) {
    this.status = status;
    updateTimestamp();
  }

  /**
   * Adds an observation to the history
   */
  public void addObservation(String observation) {
    if (observationHistory == null) {
      observationHistory = new ArrayList<>();
    }
    observationHistory.add(observation);
    updateTimestamp();
  }

  /**
   * Increments the iteration count
   */
  public void incrementIteration() {
    this.iterationCount++;
    updateTimestamp();
  }

  /**
   * Get immutable list of planning instructions
   * 
   * @return the list of planning instructions
   */
  public List<Instruction> getPlanningInstructions() {
    return Optional.ofNullable(instructions).orElseGet(() -> new ArrayList<>()).stream()
        .filter(instruction -> InstructionType.PLANNING == instruction.getType()).toList();
  }

  /**
   * Get immutable list of execution instruction by tool name
   * 
   * @param toolName
   * @return the list of corresponding execution instructions
   */
  public List<Instruction> getExecutionInstructionsOf(String toolName) {
    return Optional.ofNullable(instructions).orElseGet(() -> new ArrayList<>()).stream()
        .filter(instruction -> InstructionType.EXECUTION == instruction.getType())
        .filter(instruction -> instruction.getToolName().equals(toolName)).toList();
  }

  // Getters and setters
  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getAgentName() {
    return agentName;
  }

  public void setAgentName(String agentName) {
    this.agentName = agentName;
  }

  public ExecutionStatus getStatus() {
    return status;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
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

  public List<String> getObservationHistory() {
    return observationHistory;
  }

  public void setObservationHistory(List<String> observationHistory) {
    this.observationHistory = observationHistory;
  }

  public List<AiStep> getSteps() {
    return steps;
  }

  public void setSteps(List<AiStep> steps) {
    this.steps = steps;
  }

  public int getCurrentStepNo() {
    return currentStepNo;
  }

  public void setCurrentStepNo(int currentStepNo) {
    this.currentStepNo = currentStepNo;
    updateTimestamp();
  }

  public String getPlan() {
    return plan;
  }

  public void setPlan(String plan) {
    this.plan = plan;
  }

  public int getIterationCount() {
    return iterationCount;
  }

  public void setIterationCount(int iterationCount) {
    this.iterationCount = iterationCount;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
    updateTimestamp();
  }

  public ExecutionLogger getLogger() {
    return logger;
  }

  public void setLogger(ExecutionLogger logger) {
    this.logger = logger;
  }

  @SuppressWarnings("restriction")
  public List<CallSubStart> getTools() {
    return tools;
  }

  @SuppressWarnings("restriction")
  public void setTools(List<CallSubStart> tools) {
    this.tools = tools;
  }

  public List<Instruction> getInstructions() {
    return instructions;
  }

  public void setInstructions(List<Instruction> instructions) {
    this.instructions = instructions;
  }

  public Object getInput() {
    return input;
  }

  public void setInput(Object input) {
    this.input = input;
  }

  public String getParsedOriginalInputQuery() {
    return parsedOriginalInputQuery;
  }

  public String getGoal() {
    return goal;
  }

  public void setGoal(String goal) {
    this.goal = goal;
  }

  public Integer getMaxIteration() {
    return maxIteration;
  }

  public void setMaxIteration(Integer maxIteration) {
    this.maxIteration = maxIteration;
  }

  public AiStep getRunningStep() {
    return runningStep;
  }

  public void setRunningStep(AiStep runningStep) {
    this.runningStep = runningStep;
  }
}
