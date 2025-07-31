package com.axonivy.utils.ai.core;

import java.io.Serializable;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.axonivy.utils.ai.connector.OpenAiServiceConnector;
import com.axonivy.utils.ai.dto.ai.FieldExplanation;
import com.axonivy.utils.ai.function.DataMapper;
import com.axonivy.utils.ai.persistence.converter.BusinessEntityConverter;

public class ReActDecision implements Serializable {

  private static final long serialVersionUID = -2320942159126338114L;

  // DTO class for AI extraction
  public static class ReActDecisionData implements Serializable {
    private static final long serialVersionUID = 1L;

    private String decision; // YES, NO, or COMPLETE
    private String nextAction; // What should be the next step
    private String agentSelection; // Which tool ID should handle the next step
    private String reasoning; // Why this decision was made

    public ReActDecisionData() {
    }

    // Getters and setters
    public String getDecision() {
      return decision;
    }

    public void setDecision(String decision) {
      this.decision = decision;
    }

    public String getNextAction() {
      return nextAction;
    }

    public void setNextAction(String nextAction) {
      this.nextAction = nextAction;
    }

    public String getAgentSelection() {
      return agentSelection;
    }

    public void setAgentSelection(String agentSelection) {
      this.agentSelection = agentSelection;
    }

    public String getReasoning() {
      return reasoning;
    }

    public void setReasoning(String reasoning) {
      this.reasoning = reasoning;
    }
  }

  // Patterns to parse AI reasoning response (kept as fallback)
  private static final Pattern DECISION_PATTERN = Pattern.compile("Decision:\\s*(YES|NO|COMPLETE)",
      Pattern.CASE_INSENSITIVE);
  private static final Pattern NEXT_ACTION_PATTERN = Pattern.compile("Next Action:\\s*(.+?)(?=\\n|$)", Pattern.DOTALL);
  private static final Pattern AGENT_SELECTION_PATTERN = Pattern.compile("Agent Selection:\\s*(.+?)(?=\\n|$)",
      Pattern.DOTALL);

  private final boolean shouldUpdatePlan;
  private final boolean isComplete;
  private final String reasoning;
  private final String nextAction;
  private final String agentSelection;

  private String decisionContext; // The given context to make the decision

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

  /**
   * Parses the AI response to extract ReAct decision using DataMapping
   */
  public static ReActDecision parseReActDecision(String aiResponse) {
    try {
      // Use DataMapping to extract structured data from AI response
      DataMapper dataMapping = DataMapper.getBuilder().withObject(new ReActDecisionData())
          .useService(OpenAiServiceConnector.getTinyBrain()).withQuery(aiResponse)
          .addFieldExplanations(Arrays.asList(new FieldExplanation("decision",
              "Extract the decision value: YES (to update plan), NO (continue with current plan), or COMPLETE (goal achieved)"),
              new FieldExplanation("nextAction", "Extract the next action description when decision is YES"),
              new FieldExplanation("agentSelection", "Extract the agent/tool ID selection when decision is YES"),
              new FieldExplanation("reasoning", "Extract the reasoning or thought process behind the decision"),
              new FieldExplanation("decisionContext", "Keep this field empty")))
          .build();

      var result = dataMapping.execute();

      if (result != null && result.getSafeValue() != null) {
        // Parse the extracted data
        ReActDecisionData extracted = BusinessEntityConverter.jsonValueToEntity(result.getSafeValue(),
            ReActDecisionData.class);

        if (extracted != null) {
          String decision = extracted.getDecision() != null ? extracted.getDecision().toUpperCase() : "NO";

          boolean shouldUpdate = "YES".equals(decision);
          boolean isComplete = "COMPLETE".equals(decision);

          String nextAction = extracted.getNextAction() != null ? extracted.getNextAction().trim() : "";
          String agentSelection = extracted.getAgentSelection() != null ? extracted.getAgentSelection().trim() : "";
          String reasoning = extracted.getReasoning() != null ? extracted.getReasoning() : aiResponse;

          return new ReActDecision(shouldUpdate, isComplete, reasoning, nextAction, agentSelection);
        }
      }
    } catch (Exception e) {
      // Log the error and fall back to regex parsing
      System.out.println("DataMapping parsing failed, falling back to regex: " + e.getMessage());
    }

    // Fallback to original regex-based parsing
    return parseReActDecisionWithRegex(aiResponse);
  }

  /**
   * Fallback method using regex patterns (original implementation)
   */
  private static ReActDecision parseReActDecisionWithRegex(String aiResponse) {
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

  public String toPrettyString() {
    StringBuilder builder = new StringBuilder();
    builder.append(String.format("Reasoning: %s", reasoning));
    builder.append(System.lineSeparator());
    builder.append(String.format("Should update plan: %s", Boolean.toString(shouldUpdatePlan)));
    builder.append(System.lineSeparator());
    builder.append(String.format("Is completed: %s", Boolean.toString(isComplete)));
    builder.append(System.lineSeparator());
    builder.append(String.format("Next action: %s", nextAction));
    builder.append(System.lineSeparator());
    builder.append(String.format("Selected tool: %s", agentSelection));

    return builder.toString();
  }

  public String getDecisionContext() {
    return decisionContext;
  }

  public void setDecisionContext(String decisionContext) {
    this.decisionContext = decisionContext;
  }
}