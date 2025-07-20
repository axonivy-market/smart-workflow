package com.axonivy.utils.ai.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.ai.connector.AbstractAiServiceConnector;
import com.axonivy.utils.ai.connector.OpenAiServiceConnector;
import com.axonivy.utils.ai.core.tool.IvyTool;
import com.axonivy.utils.ai.dto.ai.AiVariable;
import com.axonivy.utils.ai.dto.ai.Instruction;
import com.axonivy.utils.ai.dto.ai.configuration.AgentModel;
import com.axonivy.utils.ai.enums.InstructionType;
import com.axonivy.utils.ai.history.HistoryLog;
import com.axonivy.utils.ai.persistence.converter.BusinessEntityConverter;
import com.axonivy.utils.ai.utils.IvyVariableUtils;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.ivyteam.ivy.environment.Ivy;

/**
 * Abstract base class for all AI agents. Contains common fields and functionality
 * that all agent types share.
 */
public abstract class BaseAgent {

  protected static final int DEFAULT_MAX_ITERATIONS = 20; // Default iteration limit
  protected static final String ONE_LINE = System.lineSeparator();
  protected static final String TWO_LINES = System.lineSeparator() + System.lineSeparator();

  // Unique identifier for the agent
  protected String id;

  // Human-readable name for the agent
  protected String name;

  // Description or intended usage of the agent
  protected String usage;

  // List of variables used or produced by the agent
  protected List<AiVariable> variables;

  // Enhanced Configuration Fields
  protected int maxIterations = DEFAULT_MAX_ITERATIONS; // Configurable iteration limit
  protected AbstractAiServiceConnector DEFAULT_CONNECTOR = OpenAiServiceConnector.getTinyBrain();

  // Dual AI Model Architecture
  protected AbstractAiServiceConnector planningModel; // For plan generation
  protected AbstractAiServiceConnector executionModel; // For step execution & reasoning

  // Instruction System
  protected List<Instruction> instructions;                 // Planning and execution instructions

  // Execution history log (not serialized to JSON)
  @JsonIgnore
  protected HistoryLog historyLog;

  protected List<String> observationHistory;

  protected String originalQuery;

  protected List<IvyTool> tools;

  // List of results collected during agent execution (not serialized to JSON)
  @JsonIgnore
  protected List<AiVariable> results;

  public BaseAgent() {
    id = UUID.randomUUID().toString().replaceAll("-", StringUtils.EMPTY);
  }

  /**
   * Load the agent from model.
   */
  public void loadFromModel(AgentModel model) {
    this.id = model.getId();
    this.name = model.getName();
    this.usage = model.getUsage();
    
    // Set configurable iterations
    // If default max iteration is not set, use the default value: 20
    this.maxIterations = model.getMaxIterations() > 0 ? model.getMaxIterations() : DEFAULT_MAX_ITERATIONS;

    // Initialize planning model
    planningModel = DEFAULT_CONNECTOR;
    if (StringUtils.isNotBlank(model.getPlanningModel()) && StringUtils.isNotBlank(model.getPlanningModelKey())) {
      planningModel = new OpenAiServiceConnector();
      planningModel.init(model.getPlanningModel(),
          IvyVariableUtils.resolveVariableReference(model.getPlanningModelKey()));
    }

    // Initialize execution model
    executionModel = DEFAULT_CONNECTOR;
    if (StringUtils.isNotBlank(model.getExecutionModel()) && StringUtils.isNotBlank(model.getExecutionModelKey())) {
      executionModel = new OpenAiServiceConnector();
      executionModel.init(model.getExecutionModel(),
          IvyVariableUtils.resolveVariableReference(model.getExecutionModelKey()));
    }

    // Load instructions
    instructions = Optional.ofNullable(model.getInstructions()).orElseGet(ArrayList::new);

    // Load tools
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
   * Helper method to filter instructions by type
   */
  protected List<String> getInstructions(InstructionType type, AiStep currentStep) {
    if (instructions == null || instructions.isEmpty()) {
      return new ArrayList<>();
    }

    return instructions.stream()
        .filter(instruction -> instruction.getType() == type)
        .filter(instruction -> StringUtils.isNotBlank(instruction.getContent().strip()))
        .filter(instruction -> instruction.getToolId().equals(currentStep.getToolId()))
        .map(Instruction::getContent)
        .collect(Collectors.toList());
  }

  protected List<String> getPlanningInstructions() {
    if (instructions == null || instructions.isEmpty()) {
      return new ArrayList<>();
    }

    return instructions.stream().filter(instruction -> instruction.getType() == InstructionType.PLANNING)
        .filter(instruction -> StringUtils.isNotBlank(instruction.getContent().strip())).map(Instruction::getContent)
        .collect(Collectors.toList());
  }

  /**
   * Builds a description of available worker agents
   */
  protected String buildAvailableToolsDescription() {
    StringBuilder toolsStr = new StringBuilder();
    for (var tool : tools) {
      toolsStr.append("- ID: ").append(tool.getId()).append(", Name: ").append(tool.getName()).append(", Usage: ")
          .append(tool.getUsage()).append(System.lineSeparator());
    }
    return toolsStr.toString();
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
   * Template method - starts the agent with a user query.
   * 
   * @param query The user input query.
   */
  public abstract void start(String query);

  /**
   * Template method - executes the agent's plan.
   */
  public abstract void execute();

  // Getters and setters
  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
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



  public int getMaxIterations() {
    return maxIterations;
  }

  public void setMaxIterations(int maxIterations) {
    this.maxIterations = maxIterations;
  }

  public List<Instruction> getInstructions() {
    return instructions;
  }

  public void setInstructions(List<Instruction> instructions) {
    this.instructions = instructions;
  }

  public AbstractAiServiceConnector getPlanningModel() {
    return planningModel;
  }

  public void setPlanningModel(AbstractAiServiceConnector planningModel) {
    this.planningModel = planningModel;
  }

  public AbstractAiServiceConnector getExecutionModel() {
    return executionModel;
  }

  public void setExecutionModel(AbstractAiServiceConnector executionModel) {
    this.executionModel = executionModel;
  }
} 