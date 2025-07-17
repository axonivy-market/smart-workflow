package com.axonivy.utils.ai.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.axonivy.utils.ai.dto.ai.AiVariable;
import com.axonivy.utils.ai.dto.ai.FieldExplanation;
import com.axonivy.utils.ai.enums.InstructionType;
import com.axonivy.utils.ai.function.DataMapping;
import com.axonivy.utils.ai.history.HistoryLog;
import com.axonivy.utils.ai.persistence.converter.BusinessEntityConverter;

import dev.langchain4j.model.input.PromptTemplate;

/**
 * Todo-based agent that works through a list of todos until each is completed.
 * This agent focuses on achieving specific outcomes rather than following predetermined steps.
 * The implicit goal of this agent is to complete all todos successfully.
 */
public class TodoAgent extends BaseAgent {

  private static final String TODO_PLANNING_TEMPLATE = """
      TASK: Create a todo list to complete the user request. Each todo should be an outcome-focused task with clear success criteria.

      {{planningInstructions}}USER QUERY: {{query}}

      Available tools: {{availableTools}}

      For each todo, provide:
      - description: What needs to be accomplished
      - successCriteria: How to determine if this todo is completed
      - availableToolIds: Which tools can work on this todo (from available tools)
      - analysis: Why this todo is necessary
      - stepNo: Sequential number starting from 1
      - resultName: Expected result name
      - resultDescription: What the result should contain

      Create a JSON list of todos that will fulfill the user request when completed sequentially.
      """;



  // List of todos to be executed
  private List<AiTodo> todos;

  // Current todo being worked on
  private AiTodo currentTodo;

  // Index of current todo
  private int currentTodoIndex = 0;

  public TodoAgent() {
    super();
    this.todos = new ArrayList<>();
  }

  /**
   * Builds todo planning prompt with planning instructions
   */
  private String buildTodoPlanningPrompt(String query) {
    // Prepare planning instructions section
    List<String> planningInstructions = getInstructionsByType(InstructionType.PLANNING);
    String planningInstructionsText = "";
    if (!planningInstructions.isEmpty()) {
      StringBuilder instructionsBuilder = new StringBuilder();
      instructionsBuilder.append("PLANNING INSTRUCTIONS:\n");
      for (int i = 0; i < planningInstructions.size(); i++) {
        instructionsBuilder.append((i + 1)).append(". ").append(planningInstructions.get(i)).append(ONE_LINE);
      }
      instructionsBuilder.append(ONE_LINE);
      planningInstructionsText = instructionsBuilder.toString();
    }

    // Build template parameters
    Map<String, Object> params = new HashMap<>();
    params.put("planningInstructions", planningInstructionsText);
    params.put("query", query);
    params.put("availableTools", buildAvailableToolsDescription());

    return PromptTemplate.from(TODO_PLANNING_TEMPLATE).apply(params).text();
  }

  /**
   * Starts the todo agent with a user query. Creates a todo list and executes each todo.
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

    // Generate todo list from query using the planning AI model
    String todoPlanningPrompt = buildTodoPlanningPrompt(query);
    
    // Use DataMapping to generate structured todos
    String todoString = DataMapping.getBuilder().useService(planningModel).withObject(new AiTodo())
        .addFieldExplanations(Arrays.asList(
            new FieldExplanation("description", "What needs to be accomplished"),
            new FieldExplanation("successCriteria", "How to determine if this todo is completed"),
            new FieldExplanation("availableToolIds", "List of tool IDs that can work on this todo"),
            new FieldExplanation("analysis", "Why this todo is necessary"),
            new FieldExplanation("stepNo", "Sequential number starting from 1"),
            new FieldExplanation("resultName", "Expected result name"),
            new FieldExplanation("resultDescription", "What the result should contain"),
            new FieldExplanation("maxIterationsPerTodo", "Maximum iterations for this todo")))
        .withQuery(todoPlanningPrompt).asList(true).build().execute().getContent();

    List<AiTodo> plannedTodos = BusinessEntityConverter.jsonValueToEntities(todoString, AiTodo.class);

    // Initialize todos
    todos = new ArrayList<>();
    for (AiTodo todo : plannedTodos) {
      // Set default values if not provided
      if (todo.getMaxIterationsPerTodo() <= 0) {
        // Max iterations of each todo equals to max iterations of the agent
        todo.setMaxIterationsPerTodo(getMaxIterations());
      }
      todos.add(todo);
    }

    execute();
  }

  /**
   * Executes the todo list sequentially until all are completed.
   */
  @Override
  public void execute() {
    if (getVariables() == null) {
      setVariables(new ArrayList<>());
    }

    historyLog = new HistoryLog();

    // Log input variables with todo information
    String inputVariablesStr = "Start running todo-based agent to complete all todos";
    if (!getVariables().isEmpty()) {
      inputVariablesStr += "\nInputs\n-----------------\n" + BusinessEntityConverter.entityToJsonValue(getVariables());
    }
    historyLog.addSystemMessage(inputVariablesStr, 0);

    int globalIterationCount = 0;

    // Execute todos sequentially
    for (int i = 0; i < todos.size() && globalIterationCount < maxIterations; i++) {
      currentTodoIndex = i;
      currentTodo = todos.get(i);

      historyLog.addSystemMessage("Starting todo " + (i + 1) + ": " + currentTodo.getDescription(), i + 1);

      // Execute this todo until completion
      boolean todoCompleted = executeTodo(currentTodo);
      globalIterationCount += currentTodo.getCurrentIteration();

      if (todoCompleted) {
        historyLog.addSystemMessage("Todo completed: " + currentTodo.getDescription(), i + 1);
        
        // Add todo results to global variables
        if (currentTodo.getTodoResults() != null) {
          getVariables().addAll(currentTodo.getTodoResults());
        }
      } else {
        historyLog.addSystemMessage("Todo failed: " + currentTodo.getDescription(), i + 1);
        // Continue to next todo or decide to stop based on configuration
      }
    }

    // Final completion check
    boolean allTodosCompleted = todos.stream().allMatch(AiTodo::isCompleted);
    if (allTodosCompleted) {
      historyLog.addSystemMessage("All todos completed successfully", todos.size());
    } else {
      historyLog.addSystemMessage("Some todos were not completed", todos.size());
    }

    if (globalIterationCount >= maxIterations) {
      historyLog.addSystemMessage("Maximum global iterations (" + maxIterations + ") reached", globalIterationCount);
    }
  }

  /**
   * Executes a single todo until completion or max iterations reached.
   */
  private boolean executeTodo(AiTodo todo) {
    while (!todo.isCompleted() && !todo.hasReachedMaxIterations()) {
      try {
        // Execute todo with current variables and tools
        List<AiVariable> todoResults = todo.execute(getVariables(), tools, executionModel);

        // Log the execution result
        String resultStr = todoResults != null ? BusinessEntityConverter.entityToJsonValue(todoResults) : "No result";
        observationHistory.add(String.format("Todo %d iteration %d: %s", 
            currentTodoIndex + 1, todo.getCurrentIteration(), resultStr));

        // Assess if todo is completed
        boolean completed = todo.assessCompletion(resultStr, executionModel);

        if (completed) {
          return true;
        }

      } catch (Exception e) {
        historyLog.addSystemMessage("Error executing todo: " + e.getMessage(), currentTodoIndex + 1);
        break;
      }
    }

    return todo.isCompleted();
  }



  // Getters and setters
  public List<AiTodo> getTodos() {
    return todos;
  }

  public void setTodos(List<AiTodo> todos) {
    this.todos = todos;
  }

  public AiTodo getCurrentTodo() {
    return currentTodo;
  }

  public int getCurrentTodoIndex() {
    return currentTodoIndex;
  }
} 