package com.axonivy.utils.ai.core.utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.axonivy.utils.ai.connector.AbstractAiServiceConnector;
import com.axonivy.utils.ai.dto.ai.AiVariable;
import com.axonivy.utils.ai.persistence.converter.BusinessEntityConverter;
import com.axonivy.utils.ai.templates.AgentTemplates;

import dev.langchain4j.model.input.PromptTemplate;

/**
 * Shared utility class for goal assessment functionality.
 * Provides common methods for determining if goals have been achieved.
 */
public class GoalAssessmentUtils {

  /**
   * Assesses whether a goal has been achieved based on current state and variables.
   * 
   * @param goal The goal to assess
   * @param originalQuery The original user query
   * @param currentState Description of the current state
   * @param variables Current variables/results
   * @param executionModel AI model to use for assessment
   * @return true if goal is achieved, false otherwise
   */
  public static boolean isGoalAchieved(String goal, String originalQuery, String currentState, 
                                      List<AiVariable> variables, AbstractAiServiceConnector executionModel) {
    try {
      // Build template parameters
      Map<String, Object> params = new HashMap<>();
      params.put("goal", goal != null ? goal : "Complete the user request");
      params.put("originalQuery", originalQuery != null ? originalQuery : "");
      params.put("currentState", currentState != null ? currentState : "");
      params.put("currentVariables", variables != null ? BusinessEntityConverter.entityToJsonValue(variables) : "[]");

      String assessmentPrompt = PromptTemplate.from(AgentTemplates.GOAL_ASSESSMENT_TEMPLATE).apply(params).text();

      // Get AI assessment
      String aiResponse = executionModel.generate(assessmentPrompt);

      // Parse the decision
      return parseGoalAchievementDecision(aiResponse);

    } catch (Exception e) {
      System.err.println("Error assessing goal achievement: " + e.getMessage());
      return false; // Conservative approach: assume goal not achieved on error
    }
  }

  /**
   * Simpler version of goal assessment with just variables.
   * 
   * @param goal The goal to assess
   * @param originalQuery The original user query
   * @param variables Current variables/results
   * @param executionModel AI model to use for assessment
   * @return true if goal is achieved, false otherwise
   */
  public static boolean isGoalAchieved(String goal, String originalQuery, List<AiVariable> variables, 
                                      AbstractAiServiceConnector executionModel) {
    return isGoalAchieved(goal, originalQuery, "Current execution state", variables, executionModel);
  }

  /**
   * Assesses task completion based on success criteria.
   * 
   * @param taskDescription Description of the task
   * @param successCriteria Criteria for task completion
   * @param actualResult The actual result achieved
   * @param executionModel AI model to use for assessment
   * @return true if task is completed, false otherwise
   */
  public static boolean isTaskCompleted(String taskDescription, String successCriteria, 
                                       String actualResult, AbstractAiServiceConnector executionModel) {
    try {
      // Build template parameters
      Map<String, Object> params = new HashMap<>();
      params.put("taskDescription", taskDescription != null ? taskDescription : "");
      params.put("successCriteria", successCriteria != null ? successCriteria : "");
      params.put("actualResult", actualResult != null ? actualResult : "No result");

      String assessmentPrompt = PromptTemplate.from(AgentTemplates.COMPLETION_VERIFICATION_TEMPLATE).apply(params)
          .text();

      // Get AI assessment
      String aiResponse = executionModel.generate(assessmentPrompt);

      // Parse the decision
      return parseTaskCompletionDecision(aiResponse);

    } catch (Exception e) {
      System.err.println("Error assessing task completion: " + e.getMessage());
      return false; // Conservative approach: assume task not completed on error
    }
  }

  /**
   * Parses the AI response for goal achievement decision.
   * 
   * @param aiResponse The response from the AI model
   * @return true if goal is achieved, false otherwise
   */
  private static boolean parseGoalAchievementDecision(String aiResponse) {
    if (aiResponse == null) {
      return false;
    }

    String upperResponse = aiResponse.toUpperCase();
    
    // Look for clear indicators of goal achievement
    return upperResponse.contains("DECISION: GOAL_ACHIEVED") || 
           upperResponse.contains("DECISION:GOAL_ACHIEVED") ||
           upperResponse.contains("GOAL_ACHIEVED");
  }

  /**
   * Parses the AI response for task completion decision.
   * 
   * @param aiResponse The response from the AI model
   * @return true if task is completed, false otherwise
   */
  private static boolean parseTaskCompletionDecision(String aiResponse) {
    if (aiResponse == null) {
      return false;
    }

    String upperResponse = aiResponse.toUpperCase();
    
    // Look for clear indicators of task completion
    return upperResponse.contains("DECISION: COMPLETED") || 
           upperResponse.contains("DECISION:COMPLETED") ||
           (upperResponse.contains("COMPLETED") && !upperResponse.contains("NOT_COMPLETED"));
  }

  /**
   * Extracts reasoning from the AI response for debugging purposes.
   * 
   * @param aiResponse The response from the AI model
   * @return The reasoning portion of the response, or the full response if parsing fails
   */
  public static String extractReasoning(String aiResponse) {
    if (aiResponse == null || aiResponse.trim().isEmpty()) {
      return "No reasoning provided";
    }

    // Try to extract reasoning section
    try {
      String[] lines = aiResponse.split("\n");
      StringBuilder reasoning = new StringBuilder();
      boolean inReasoningSection = false;

      for (String line : lines) {
        String trimmedLine = line.trim();
        if (trimmedLine.toLowerCase().startsWith("reasoning:")) {
          inReasoningSection = true;
          reasoning.append(trimmedLine.substring(10).trim()).append("\n");
        } else if (inReasoningSection && !trimmedLine.isEmpty() && !trimmedLine.contains(":")) {
          reasoning.append(trimmedLine).append("\n");
        } else if (inReasoningSection && trimmedLine.contains(":")) {
          break; // End of reasoning section
        }
      }

      return reasoning.length() > 0 ? reasoning.toString().trim() : aiResponse;

    } catch (Exception e) {
      return aiResponse; // Return full response if parsing fails
    }
  }

  // Private constructor to prevent instantiation
  private GoalAssessmentUtils() {
    throw new UnsupportedOperationException("GoalAssessmentUtils is a utility class and should not be instantiated");
  }
} 