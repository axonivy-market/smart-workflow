package com.axonivy.utils.smart.workflow.demo.model;

import java.util.List;

import dev.langchain4j.model.output.structured.Description;

@Description("Job fit assessment score for a candidate")
public class FitScore {

  @Description("Overall fit score from 0 to 100")
  private int score;

  @Description("True if the candidate meets minimum requirements and should proceed to interview")
  private boolean qualified;

  @Description("Key strengths of the candidate relevant to the position")
  private List<String> strengths;

  @Description("Gaps or missing qualifications")
  private List<String> gaps;

  @Description("One-line recommendation summary")
  private String recommendation;

  public int getScore() {
    return score;
  }

  public void setScore(int score) {
    this.score = score;
  }

  public boolean isQualified() {
    return qualified;
  }

  public void setQualified(boolean qualified) {
    this.qualified = qualified;
  }

  public List<String> getStrengths() {
    return strengths;
  }

  public void setStrengths(List<String> strengths) {
    this.strengths = strengths;
  }

  public List<String> getGaps() {
    return gaps;
  }

  public void setGaps(List<String> gaps) {
    this.gaps = gaps;
  }

  public String getRecommendation() {
    return recommendation;
  }

  public void setRecommendation(String recommendation) {
    this.recommendation = recommendation;
  }
}
