package com.axonivy.utils.ai.core;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.ai.core.log.ExecutionLogger;
import com.axonivy.utils.ai.dto.ai.AiVariable;
import com.axonivy.utils.ai.enums.ExecutionStatus;
import com.axonivy.utils.ai.persistence.converter.BusinessEntityConverter;
import com.axonivy.utils.ai.utils.IdGenerationUtils;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Represents a single execution instance of an AI agent.
 * Contains all state information needed to track and manage
 * an agent's execution lifecycle.
 */
public class AgentExecution implements Serializable {

  private static final long serialVersionUID = -7438395505679440025L;

//Execution metadata
  private String id;
  private String agentId;
  private ExecutionStatus status;
  private String username;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  // Execution state (moved from BaseAgent)
  private String originalQuery;
  private List<AiVariable> variables;
  private List<AiVariable> results;
  private List<String> observationHistory;

  // Agent-specific execution state (moved from IvyAgent)
  private List<AiStep> steps;
  private int currentStepNo;
  private String plan;

  // Execution control
  private int iterationCount;
  private String errorMessage;

  @JsonIgnore
  private ExecutionLogger logger;

  /**
   * Constructor with basic execution info
   */
  public AgentExecution(String agentId, String originalQuery, String username) {
    this.id = IdGenerationUtils.generateRandomId();
    this.agentId = agentId;
    this.originalQuery = originalQuery;
    this.username = username;
    this.status = ExecutionStatus.PENDING;
    this.createdAt = LocalDateTime.now();
    this.updatedAt = LocalDateTime.now();
    this.variables = new ArrayList<>();
    this.results = new ArrayList<>();
    this.observationHistory = new ArrayList<>();
    this.steps = new ArrayList<>();
    this.currentStepNo = AiStep.INITIAL_STEP;
    this.iterationCount = 0;

    // Use the query to initialize variables.
    processInputQuery(originalQuery);

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
  protected void processInputQuery(String query) {
    if (getVariables() == null) {
      setVariables(new ArrayList<>());
    }

    // Try to parse as JSON object first
    if (isValidJsonObject(query)) {
      createVariablesFromJsonObject(query);
    } else {
      // Fall back to single query variable for everything else
      // (arrays, primitives, invalid JSON, plain text)
      createSingleQueryVariable(query);
    }
  }

  /**
   * Checks if the input string is a valid JSON object (not array or primitive)
   */
  private boolean isValidJsonObject(String input) {
    if (StringUtils.isBlank(input)) {
      return false;
    }

    try {
      ObjectMapper mapper = BusinessEntityConverter.getObjectMapper();
      JsonNode rootNode = mapper.readTree(input.trim());
      // Only return true for JSON objects, not arrays or primitives
      return rootNode.isObject();
    } catch (Exception e) {
      return false;
    }
  }

  /**
   * Creates variables from JSON object input. Only handles JSON objects.
   */
  private void createVariablesFromJsonObject(String jsonInput) {
    try {
      ObjectMapper mapper = BusinessEntityConverter.getObjectMapper();
      JsonNode rootNode = mapper.readTree(jsonInput.trim());

      // Handle JSON object - create variable for each property
      rootNode.fieldNames().forEachRemaining(fieldName -> {
        JsonNode fieldValue = rootNode.get(fieldName);
        AiVariable variable = new AiVariable();
        variable.init();
        variable.getParameter().setName(fieldName);
        variable.getParameter().setValue(fieldValue.isTextual() ? fieldValue.asText() : fieldValue.toString());
        variable.getParameter().setDescription("JSON property: " + fieldName);

        // By default, all parameters send to agent are String
        variable.getParameter().setClassName("String");

        getVariables().add(variable);
      });
    } catch (Exception e) {
      // If JSON parsing fails unexpectedly, fall back to single query variable
      createSingleQueryVariable(jsonInput);
    }
  }

  /**
   * Creates a single query variable with the entire input
   */
  private void createSingleQueryVariable(String query) {
    AiVariable inputVariable = new AiVariable();
    inputVariable.init();
    inputVariable.getParameter().setName("query");
    inputVariable.getParameter().setValue(query);
    inputVariable.getParameter().setDescription("The input query");
    getVariables().add(inputVariable);
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
   * Gets the current step based on currentStepNo
   */
  public AiStep getCurrentStep() {
    if (steps == null || steps.isEmpty()) {
      return null;
    }

    return steps.stream()
        .filter(step -> currentStepNo == AiStep.INITIAL_STEP ? step.getPrevious() == AiStep.INITIAL_STEP
            : step.getStepNo() == currentStepNo)
        .findFirst().orElse(null);
  }

  // Getters and setters
  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getAgentId() {
    return agentId;
  }

  public void setAgentId(String agentId) {
    this.agentId = agentId;
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

  public String getOriginalQuery() {
    return originalQuery;
  }

  public void setOriginalQuery(String originalQuery) {
    this.originalQuery = originalQuery;
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
}
