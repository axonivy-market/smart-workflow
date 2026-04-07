package com.axonivy.utils.smart.workflow.rag.entity;

import java.util.List;

public class RagResult {

  private String answer;
  private List<RagMatch> matches;
  private String error;

  public RagResult() {
  }

  public RagResult(List<RagMatch> matches) {
    this.matches = matches;
  }

  public RagResult(String error) {
    this.error = error;
  }

  public String getAnswer() {
    return answer;
  }

  public void setAnswer(String answer) {
    this.answer = answer;
  }

  public List<RagMatch> getMatches() {
    return matches;
  }

  public void setMatches(List<RagMatch> matches) {
    this.matches = matches;
  }

  public String getError() {
    return error;
  }

  public void setError(String error) {
    this.error = error;
  }

  public boolean hasError() {
    return error != null && !error.isBlank();
  }

}
