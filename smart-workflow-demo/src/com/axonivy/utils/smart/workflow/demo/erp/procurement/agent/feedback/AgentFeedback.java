package com.axonivy.utils.smart.workflow.demo.erp.procurement.agent.feedback;

import java.io.Serializable;

import dev.langchain4j.model.output.structured.Description;

public class AgentFeedback implements Serializable {

  private static final long serialVersionUID = 1L;

  @Description("The ID of the item this feedback refers to (e.g., the material item ID being analyzed).")
  private String id;

  @Description("A message describing the feedback from the agent, such as success or error details.")
  private String message;

  @Description("Whether this feedback represents a successful outcome.")
  private boolean success;

  @Description("The type of feedback provided by the agent.")
  private FeedbackType type;

  @Description("If the feedback type is OPTION, this field contains the details of the feedback option.")
  private FeedbackOption feedbackOption;

  @Description("Full ranked analysis of all evaluated alternatives, produced before the final selection.")
  private String rankingAnalysis;

  public String getId() { return id; }
  public void setId(String id) { this.id = id; }

  public String getMessage() { return message; }
  public void setMessage(String message) { this.message = message; }

  public boolean isSuccess() { return success; }
  public void setSuccess(boolean success) { this.success = success; }

  public FeedbackType getType() { return type; }
  public void setType(FeedbackType type) { this.type = type; }

  public FeedbackOption getFeedbackOption() { return feedbackOption; }
  public void setFeedbackOption(FeedbackOption feedbackOption) { this.feedbackOption = feedbackOption; }

  public String getRankingAnalysis() { return rankingAnalysis; }
  public void setRankingAnalysis(String rankingAnalysis) { this.rankingAnalysis = rankingAnalysis; }
}
