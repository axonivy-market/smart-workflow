package com.axonivy.utils.ai.core;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.ai.connector.AbstractAiServiceConnector;
import com.axonivy.utils.ai.core.tool.IvyTool;
import com.axonivy.utils.ai.dto.ai.AiVariable;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import dev.langchain4j.model.input.PromptTemplate;

/**
 * Represents a single todo item in a todo-based agent execution strategy.
 * Each todo has a specific objective, success criteria, and can be executed
 * using multiple iterations until completion.
 */
@JsonInclude(value = Include.NON_EMPTY)
public class AiTodo implements Serializable {

  private static final long serialVersionUID = -1234567890123456789L;

  // Todo completion assessment prompt
  private static final String COMPLETION_ASSESSMENT_TEMPLATE = """
      TODO: {{todoDescription}}
      SUCCESS CRITERIA: {{successCriteria}}
      
      LATEST EXECUTION RESULT:
      {{latestResult}}
      
      EVALUATION REQUIRED:
      1. Review the latest execution result
      2. Compare it against the success criteria
      3. Determine if this todo is now completed
      
      Based on the success criteria and latest result, is this todo completed?
      
      Analysis: [Analyze how the result relates to the success criteria]
      Decision: [COMPLETED | NOT_COMPLETED]
      Reasoning: [Explain why the todo is or isn't completed]
      
      If NOT_COMPLETED, provide:
      Next Steps: [What should be done next to complete this todo]
      """;

  // Constants for todo status
  public static final String STATUS_PENDING = "pending";
  public static final String STATUS_IN_PROGRESS = "in_progress";
  public static final String STATUS_COMPLETED = "completed";
  public static final String STATUS_FAILED = "failed";

  // Unique identifier for this todo
  private String id;

  // Description of what needs to be accomplished
  private String description;

  // Criteria that define when this todo is successfully completed
  private String successCriteria;

  // Current completion status
  private boolean isCompleted = false;

  // Current status of the todo
  private String currentStatus = STATUS_PENDING;

  // List of tool IDs that can work on this todo
  private List<String> availableToolIds;

  // Maximum iterations allowed for this specific todo
  private int maxIterationsPerTodo = 5;

  // Results accumulated during todo execution
  @JsonIgnore
  private List<AiVariable> todoResults;

  // Current iteration count for this todo
  @JsonIgnore
  private int currentIteration = 0;

  // Step number for tracking (similar to AiStep)
  private int stepNo;

  // Analysis of the todo (why it's needed)
  private String analysis;

  // Expected result name
  private String resultName;

  // Expected result description  
  private String resultDescription;

  public AiTodo() {
    this.id = UUID.randomUUID().toString().replaceAll("-", StringUtils.EMPTY);
    this.todoResults = new ArrayList<>();
    this.availableToolIds = new ArrayList<>();
  }

  /**
   * Executes this todo using the provided variables, tools, and execution model.
   * Will attempt execution and return the results.
   * 
   * @param variables Current variables available for execution
   * @param allTools All available tools  
   * @param executionModel AI model for execution
   * @return The variables produced by this todo execution
   */
  public List<AiVariable> execute(List<AiVariable> variables, List<IvyTool> allTools, 
                                 AbstractAiServiceConnector executionModel) {
    
    if (isCompleted) {
      return todoResults; // Already completed, return existing results
    }

    currentStatus = STATUS_IN_PROGRESS;
    currentIteration++;

    try {
      // Find a suitable tool for this todo
      IvyTool selectedTool = selectBestTool(allTools);
      if (selectedTool == null) {
        currentStatus = STATUS_FAILED;
        return new ArrayList<>();
      }

      // Execute using the selected tool
      selectedTool.setConnector(executionModel);
      List<AiVariable> executionResults = selectedTool.execute(variables);

      if (executionResults != null) {
        todoResults.addAll(executionResults);
      }

      return executionResults;

    } catch (Exception e) {
      System.err.println("Error executing todo '" + id + "': " + e.getMessage());
      currentStatus = STATUS_FAILED;
      return new ArrayList<>();
    }
  }

  /**
   * Assesses whether this todo has been completed based on the latest execution result.
   * 
   * @param latestResult The result from the latest execution
   * @param executionModel AI model to use for assessment
   * @return true if the todo is completed, false otherwise
   */
  public boolean assessCompletion(String latestResult, AbstractAiServiceConnector executionModel) {
    if (isCompleted) {
      return true; // Already completed
    }

    try {
      // Build assessment prompt
      Map<String, Object> params = new HashMap<>();
      params.put("todoDescription", description != null ? description : "");
      params.put("successCriteria", successCriteria != null ? successCriteria : "");
      params.put("latestResult", latestResult != null ? latestResult : "No result");

      String assessmentPrompt = PromptTemplate.from(COMPLETION_ASSESSMENT_TEMPLATE).apply(params).text();

      // Get AI assessment
      String aiResponse = executionModel.generate(assessmentPrompt);

      // Parse the decision
      boolean completed = aiResponse.toUpperCase().contains("DECISION: COMPLETED") || 
                         aiResponse.toUpperCase().contains("DECISION:COMPLETED");

      if (completed) {
        isCompleted = true;
        currentStatus = STATUS_COMPLETED;
      }

      return completed;

    } catch (Exception e) {
      System.err.println("Error assessing todo completion for '" + id + "': " + e.getMessage());
      return false;
    }
  }

  /**
   * Selects the best tool for executing this todo based on available tool IDs.
   */
  private IvyTool selectBestTool(List<IvyTool> allTools) {
    if (availableToolIds == null || availableToolIds.isEmpty()) {
      // If no specific tools are specified, return the first available tool
      return allTools.isEmpty() ? null : allTools.get(0);
    }

    // Find a tool that matches our available tool IDs
    for (String toolId : availableToolIds) {
      for (IvyTool tool : allTools) {
        if (tool.getId().equals(toolId)) {
          return tool;
        }
      }
    }

    // Fallback: return first available tool if none match
    return allTools.isEmpty() ? null : allTools.get(0);
  }

  /**
   * Resets the todo for re-execution
   */
  public void reset() {
    isCompleted = false;
    currentStatus = STATUS_PENDING;
    currentIteration = 0;
    if (todoResults != null) {
      todoResults.clear();
    }
  }

  /**
   * Checks if this todo has reached its maximum iteration limit
   */
  public boolean hasReachedMaxIterations() {
    return currentIteration >= maxIterationsPerTodo;
  }

  // Getters and setters
  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getSuccessCriteria() {
    return successCriteria;
  }

  public void setSuccessCriteria(String successCriteria) {
    this.successCriteria = successCriteria;
  }

  public boolean isCompleted() {
    return isCompleted;
  }

  public void setCompleted(boolean completed) {
    isCompleted = completed;
    if (completed) {
      currentStatus = STATUS_COMPLETED;
    }
  }

  public String getCurrentStatus() {
    return currentStatus;
  }

  public void setCurrentStatus(String currentStatus) {
    this.currentStatus = currentStatus;
  }

  public List<String> getAvailableToolIds() {
    return availableToolIds;
  }

  public void setAvailableToolIds(List<String> availableToolIds) {
    this.availableToolIds = availableToolIds;
  }

  public int getMaxIterationsPerTodo() {
    return maxIterationsPerTodo;
  }

  public void setMaxIterationsPerTodo(int maxIterationsPerTodo) {
    this.maxIterationsPerTodo = maxIterationsPerTodo;
  }

  public List<AiVariable> getTodoResults() {
    return todoResults;
  }

  public void setTodoResults(List<AiVariable> todoResults) {
    this.todoResults = todoResults;
  }

  public int getCurrentIteration() {
    return currentIteration;
  }

  public void setCurrentIteration(int currentIteration) {
    this.currentIteration = currentIteration;
  }

  public int getStepNo() {
    return stepNo;
  }

  public void setStepNo(int stepNo) {
    this.stepNo = stepNo;
  }

  public String getAnalysis() {
    return analysis;
  }

  public void setAnalysis(String analysis) {
    this.analysis = analysis;
  }

  public String getResultName() {
    return resultName;
  }

  public void setResultName(String resultName) {
    this.resultName = resultName;
  }

  public String getResultDescription() {
    return resultDescription;
  }

  public void setResultDescription(String resultDescription) {
    this.resultDescription = resultDescription;
  }
} 