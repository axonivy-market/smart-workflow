package com.axonivy.utils.ai.templates;

/**
 * Shared prompt templates that can be used by different agent types. This
 * centralizes common prompt patterns and makes them reusable.
 */
public final class AgentTemplates {
  /**
   * Template for assessing goal achievement
   */
  public static final String GOAL_ASSESSMENT_TEMPLATE = """
      GOAL: {{goal}}
      ORIGINAL QUERY: {{originalQuery}}

      CURRENT STATE:
      {{currentState}}

      CURRENT VARIABLES:
      {{currentVariables}}

      ASSESSMENT REQUIRED:
      1. Review the current state and available variables
      2. Check if the original goal has been achieved
      3. Determine if any additional work is needed

      Based on the current state and variables, has the goal been achieved?

      Analysis: [Analyze how the current state relates to the original goal]
      Decision: [GOAL_ACHIEVED | CONTINUE_NEEDED]
      Reasoning: [Explain why the goal is or isn't achieved]

      If CONTINUE_NEEDED, provide:
      Missing: [What is still needed to achieve the goal]
      """;

  /**
   * Template for building planning prompts with instructions
   */
  public static final String PLANNING_BASE_TEMPLATE = """
      GOAL: {{goal}}

      {{planningInstructions}}USER QUERY: {{query}}

      Available tools: {{availableTools}}

      {{specificInstructions}}
      """;

  /**
   * Template for execution reasoning and decision making
   */
  public static final String EXECUTION_REASONING_TEMPLATE = """
      GOAL: {{goal}}

      {{executionInstructions}}CURRENT SITUATION:
      Original Query: {{originalQuery}}
      Current Context: {{currentContext}}
      Latest Result: {{latestResult}}
      Observation History:
      {{observationHistory}}

      AVAILABLE TOOLS:
      {{availableTools}}

      ANALYSIS REQUIRED:
      1. Is the goal achieved? (Consider the execution instructions)
      2. If not, should we continue with the current approach or adapt it?
      3. Are there any execution instruction triggers based on the latest result?

      Based on the latest result and current progress, provide your analysis:

      Thought: [What is the current situation and what should be done next?]
      Reasoning: [Analyze the result and determine the best course of action]
      Decision: [What should happen next?]
      """;

  /**
   * Template for error handling and recovery
   */
  public static final String ERROR_RECOVERY_TEMPLATE = """
      GOAL: {{goal}}
      ERROR OCCURRED: {{errorMessage}}
      CURRENT CONTEXT: {{currentContext}}

      RECOVERY ANALYSIS:
      1. What went wrong?
      2. Can we recover from this error?
      3. What should be the next step?

      Provide a recovery strategy:
      Analysis: [What caused the error and why]
      Recovery: [RETRY | SKIP | ALTERNATIVE | ABORT]
      NextAction: [What should be done to recover]
      """;

  /**
   * Template for completion verification
   */
  public static final String COMPLETION_VERIFICATION_TEMPLATE = """
      TASK: {{taskDescription}}
      SUCCESS CRITERIA: {{successCriteria}}

      ACTUAL RESULT:
      {{actualResult}}

      VERIFICATION REQUIRED:
      1. Compare the actual result against the success criteria
      2. Determine if the task has been successfully completed
      3. Identify any gaps or missing elements

      Based on the success criteria and actual result:

      Analysis: [How does the result compare to the criteria?]
      Decision: [COMPLETED | NOT_COMPLETED]
      Reasoning: [Why is the task completed or not completed?]

      If NOT_COMPLETED, provide:
      Gaps: [What is missing or incorrect]
      NextSteps: [What should be done to complete the task]
      """;
}
