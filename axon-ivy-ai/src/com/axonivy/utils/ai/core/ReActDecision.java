package com.axonivy.utils.ai.core;

import java.io.Serializable;

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

  private boolean isComplete;
  private String reasoning;
  private String nextAction;
  private String selectedSignature;

  private String decisionContext; // The given context to make the decision

  public ReActDecision() {
  }

  public ReActDecision(boolean shouldUpdatePlan, boolean isComplete, String reasoning, String nextAction,
      String selectedSignature, String decisionContext) {
    this.isComplete = isComplete;
    this.reasoning = reasoning;
    this.nextAction = nextAction;
    this.selectedSignature = selectedSignature;
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

  public String getSelectedSignature() {
    return selectedSignature;
  }

  public String toPrettyString() {
    StringBuilder builder = new StringBuilder();
    builder.append(String.format("Reasoning: %s", reasoning));
    builder.append(System.lineSeparator());
    builder.append(String.format("Is completed: %s", Boolean.toString(isComplete)));
    builder.append(System.lineSeparator());
    builder.append(String.format("Next action: %s", nextAction));
    builder.append(System.lineSeparator());
    builder.append(String.format("Signature of selected tool: %s", selectedSignature));

    return builder.toString();
  }

  public String getDecisionContext() {
    return decisionContext;
  }

  public void setDecisionContext(String decisionContext) {
    this.decisionContext = decisionContext;
  }
}