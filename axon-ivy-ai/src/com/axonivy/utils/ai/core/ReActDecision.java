package com.axonivy.utils.ai.core;

import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReActDecision implements Serializable {

  private static final long serialVersionUID = -2320942159126338114L;

  // Patterns to parse AI reasoning response
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
   * Parses the AI response to extract ReAct decision
   */
  public static ReActDecision parseReActDecision(String aiResponse) {
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
}