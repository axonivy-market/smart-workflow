package com.axonivy.utils.ai.axon.ivy.ai.demo.dto;

import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.ai.axon.ivy.ai.demo.enums.TicketType;
import com.fasterxml.jackson.annotation.JsonIgnore;

public class SupportTicket {
  private String id;
  private TicketType type;
  private String name;
  private String description;
  private String requestorId;
  private String firstApprover;
  private String secondApprover;

  @JsonIgnore
  private Employee requestor;

  public SupportTicket() {
    setId(UUID.randomUUID().toString().replaceAll("-", StringUtils.EMPTY));
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public TicketType getType() {
    return type;
  }

  public void setType(TicketType type) {
    this.type = type;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getRequestorId() {
    return requestorId;
  }

  public void setRequestorId(String requestorId) {
    this.requestorId = requestorId;
  }

  public String getFirstApprover() {
    return firstApprover;
  }

  public void setFirstApprover(String firstApprover) {
    this.firstApprover = firstApprover;
  }

  public String getSecondApprover() {
    return secondApprover;
  }

  public void setSecondApprover(String secondApprover) {
    this.secondApprover = secondApprover;
  }

  public Employee getRequestor() {
    return requestor;
  }

  public void setRequestor(Employee requestor) {
    this.requestor = requestor;
  }
}