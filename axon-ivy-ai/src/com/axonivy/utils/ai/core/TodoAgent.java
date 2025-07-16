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
 */
public class TodoAgent extends BaseAgent {

  private static final String TODO_PLANNING_TEMPLATE = """
      GOAL: {{goal}}

      {{planningInstructions}}USER QUERY: {{query}}

      Available tools: {{availableTools}}

      TASK: Create a todo list to achieve the goal. Each todo should be an outcome-focused task with clear success criteria.

      For each todo, provide:
      - description: What needs to be accomplished
      - successCriteria: How to determine if this todo is completed
      - availableToolIds: Which tools can work on this todo (from available tools)
      - analysis: Why this todo is necessary
      - stepNo: Sequential number starting from 1
      - resultName: Expected result name
      - resultDescription: What the result should contain

      Create a JSON list of todos that will achieve the goal when completed sequentially.
      """;

  private static final String GOAL_ASSESSMENT_TEMPLATE = """
      GOAL: {{goal}}
      ORIGINAL QUERY: {{originalQuery}}

      COMPLETED TODOS:
      {{completedTodos}}

      CURRENT VARIABLES:
      {{currentVariables}}

      ASSESSMENT REQUIRED:
      1. Review all completed todos and their results
      2. Check if the original goal has been achieved
      3. Determine if any additional work is needed

      Based on the completed todos and current state, has the goal been achieved?

      Analysis: [Analyze how the completed todos relate to the original goal]
      Decision: [GOAL_ACHIEVED | CONTINUE_NEEDED]
      Reasoning: [Explain why the goal is or isn't achieved]

      If CONTINUE_NEEDED, provide:
      Missing: [What is still needed to achieve the goal]
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
   * Builds todo planning prompt that includes goal and planning instructions
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
    params.put("goal", goal != null ? goal : "Complete the user request");
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
            new FieldExplanation("maxIterationsPerTodo", "Maximum iterations for this todo (default 5)")))
        .withQuery(todoPlanningPrompt).asList(true).build().execute().getContent();

    List<AiTodo> plannedTodos = BusinessEntityConverter.jsonValueToEntities(todoString, AiTodo.class);

    // Initialize todos
    todos = new ArrayList<>();
    for (AiTodo todo : plannedTodos) {
      // Set default values if not provided
      if (todo.getMaxIterationsPerTodo() <= 0) {
        todo.setMaxIterationsPerTodo(5);
      }
      todos.add(todo);
    }

    execute();
  }

  /**
   * Executes the todo list sequentially until all are completed or goal is achieved.
   */
  @Override
  public void execute() {
    if (getVariables() == null) {
      setVariables(new ArrayList<>());
    }

    historyLog = new HistoryLog();

    // Log input variables with goal information
    String inputVariablesStr = "Start running todo-based agent";
    if (goal != null && !goal.isEmpty()) {
      inputVariablesStr += " with goal: " + goal;
    }
    if (!getVariables().isEmpty()) {
      inputVariablesStr += "\nInputs\n-----------------\n" + BusinessEntityConverter.entityToJsonValue(getVariables());
    }
    historyLog.addSystemMessage(inputVariablesStr, 0);

    int globalIterationCount = 0;

    // Execute todos sequentially
    for (int i = 0; i < todos.size() && globalIterationCount < maxIterations; i++) {
      currentTodoIndex = i;
      currentTodo = todos.get(i);

      System.err.println("Starting todo " + (i + 1) + ": " + currentTodo.getDescription());
      historyLog.addSystemMessage("Starting todo " + (i + 1) + ": " + currentTodo.getDescription(), i + 1);

      // Execute this todo until completion
      boolean todoCompleted = executeTodo(currentTodo);
      globalIterationCount += currentTodo.getCurrentIteration();

      if (todoCompleted) {
        System.err.println("Todo completed: " + currentTodo.getDescription());
        historyLog.addSystemMessage("Todo completed: " + currentTodo.getDescription(), i + 1);
        
        // Add todo results to global variables
        if (currentTodo.getTodoResults() != null) {
          getVariables().addAll(currentTodo.getTodoResults());
        }

        // Check if overall goal is achieved after this todo
        if (isGoalAchieved()) {
          System.err.println("Goal achieved after completing todo " + (i + 1));
          historyLog.addSystemMessage("Goal achieved after completing todo " + (i + 1), i + 1);
          break;
        }
      } else {
        System.err.println("Todo failed or reached max iterations: " + currentTodo.getDescription());
        historyLog.addSystemMessage("Todo failed: " + currentTodo.getDescription(), i + 1);
        // Continue to next todo or decide to stop based on configuration
      }
    }

    // Final goal verification
    if (!isGoalAchieved()) {
      System.err.println("All todos completed but goal may not be fully achieved");
      historyLog.addSystemMessage("All todos completed but goal may not be fully achieved", todos.size());
    }

    if (globalIterationCount >= maxIterations) {
      System.err.println("Maximum global iterations (" + maxIterations + ") reached in todo execution");
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

        // Add a small delay to prevent rapid iteration (optional)
        try {
          Thread.sleep(100);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          break;
        }

      } catch (Exception e) {
        System.err.println("Error executing todo '" + todo.getDescription() + "': " + e.getMessage());
        historyLog.addSystemMessage("Error executing todo: " + e.getMessage(), currentTodoIndex + 1);
        break;
      }
    }

    return todo.isCompleted();
  }

  /**
   * Checks if the overall goal has been achieved based on completed todos.
   */
  private boolean isGoalAchieved() {
    try {
      // Build completed todos summary
      StringBuilder completedTodosStr = new StringBuilder();
      for (int i = 0; i <= currentTodoIndex && i < todos.size(); i++) {
        AiTodo todo = todos.get(i);
        if (todo.isCompleted()) {
          completedTodosStr.append("- ").append(todo.getDescription())
              .append(" (Completed: ").append(todo.getCurrentStatus()).append(")\n");
        }
      }

      // Build template parameters
      Map<String, Object> params = new HashMap<>();
      params.put("goal", goal != null ? goal : "Complete the user request");
      params.put("originalQuery", originalQuery);
      params.put("completedTodos", completedTodosStr.toString());
      params.put("currentVariables", BusinessEntityConverter.entityToJsonValue(getVariables()));

      String assessmentPrompt = PromptTemplate.from(GOAL_ASSESSMENT_TEMPLATE).apply(params).text();

      // Get AI assessment
      String aiResponse = executionModel.generate(assessmentPrompt);

      // Parse the decision
      boolean goalAchieved = aiResponse.toUpperCase().contains("DECISION: GOAL_ACHIEVED") || 
                           aiResponse.toUpperCase().contains("DECISION:GOAL_ACHIEVED");

      return goalAchieved;

    } catch (Exception e) {
      System.err.println("Error assessing goal achievement: " + e.getMessage());
      return false; // Conservative approach: assume goal not achieved on error
    }
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