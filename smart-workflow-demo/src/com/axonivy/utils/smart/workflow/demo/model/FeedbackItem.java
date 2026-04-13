package com.axonivy.utils.smart.workflow.demo.model;

import dev.langchain4j.model.output.structured.Description;

@Description("A single feedback item extracted from a meeting transcript")
public class FeedbackItem {

  @Description("Short title summarizing the feedback (max 10 words)")
  private String title;

  @Description("Detailed description of the feedback, bug, or feature request")
  private String description;

  @Description("Type of feedback: bug, feature, or improvement")
  private String type;

  @Description("Username of the person to assign this task to, based on who raised the issue in the transcript. Leave empty if unclear.")
  private String assignee;

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getAssignee() {
    return assignee;
  }

  public void setAssignee(String assignee) {
    this.assignee = assignee;
  }
}
