package com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding;

public class NotificationRecord {

  private String recipientName;
  private String recipientRole;
  private String channel;
  private String sentAt;
  private String status;

  public String getRecipientName() {
    return recipientName;
  }

  public void setRecipientName(String recipientName) {
    this.recipientName = recipientName;
  }

  public String getRecipientRole() {
    return recipientRole;
  }

  public void setRecipientRole(String recipientRole) {
    this.recipientRole = recipientRole;
  }

  public String getChannel() {
    return channel;
  }

  public void setChannel(String channel) {
    this.channel = channel;
  }

  public String getSentAt() {
    return sentAt;
  }

  public void setSentAt(String sentAt) {
    this.sentAt = sentAt;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }
}
