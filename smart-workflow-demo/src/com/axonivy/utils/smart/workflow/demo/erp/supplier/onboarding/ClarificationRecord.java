package com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding;

import java.util.List;

public class ClarificationRecord {

  private int cycle;
  private String submittedAt;
  private String submittedBy;
  private String submittedByRole;
  private String additionalNotes;
  private List<String> itemsAddressed;
  private boolean escalated;

  public int getCycle() {
    return cycle;
  }

  public void setCycle(int cycle) {
    this.cycle = cycle;
  }

  public String getSubmittedAt() {
    return submittedAt;
  }

  public void setSubmittedAt(String submittedAt) {
    this.submittedAt = submittedAt;
  }

  public String getSubmittedBy() {
    return submittedBy;
  }

  public void setSubmittedBy(String submittedBy) {
    this.submittedBy = submittedBy;
  }

  public String getSubmittedByRole() {
    return submittedByRole;
  }

  public void setSubmittedByRole(String submittedByRole) {
    this.submittedByRole = submittedByRole;
  }

  public String getAdditionalNotes() {
    return additionalNotes;
  }

  public void setAdditionalNotes(String additionalNotes) {
    this.additionalNotes = additionalNotes;
  }

  public List<String> getItemsAddressed() {
    return itemsAddressed;
  }

  public void setItemsAddressed(List<String> itemsAddressed) {
    this.itemsAddressed = itemsAddressed;
  }

  public boolean isEscalated() {
    return escalated;
  }

  public void setEscalated(boolean escalated) {
    this.escalated = escalated;
  }
}
