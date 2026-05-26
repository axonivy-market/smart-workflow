package com.axonivy.utils.smart.workflow.rag.pipeline;

import java.util.Map;

public class RagMatch {

  private String content;
  private double score;
  private Map<String, String> metadata;

  public RagMatch() {
  }

  public RagMatch(String content, double score, Map<String, String> metadata) {
    this.content = content;
    this.score = score;
    this.metadata = metadata;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public double getScore() {
    return score;
  }

  public void setScore(double score) {
    this.score = score;
  }

  public Map<String, String> getMetadata() {
    return metadata;
  }

  public void setMetadata(Map<String, String> metadata) {
    this.metadata = metadata;
  }

}
