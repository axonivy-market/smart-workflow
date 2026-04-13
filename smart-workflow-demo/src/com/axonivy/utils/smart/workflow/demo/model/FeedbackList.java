package com.axonivy.utils.smart.workflow.demo.model;

import java.util.List;

import dev.langchain4j.model.output.structured.Description;

@Description("A list of feedback items extracted from a sprint review meeting transcript")
public class FeedbackList {

  @Description("All feedback items extracted: bugs, feature requests, and improvement suggestions")
  private List<FeedbackItem> items;

  public List<FeedbackItem> getItems() {
    return items;
  }

  public void setItems(List<FeedbackItem> items) {
    this.items = items;
  }
}
