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
  private String employeeUsername;
  private String firstApprover;
  private String secondApprover;
  private ApprovalHistory firstApproval;
  private ApprovalHistory secondApproval;


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

  public String getEmployeeUsername() {
    return employeeUsername;
  }

  public void setEmployeeUsername(String employeeUsername) {
    this.employeeUsername = employeeUsername;
  }

  public ApprovalHistory getFirstApproval() {
    return firstApproval;
  }

  public void setFirstApproval(ApprovalHistory firstApproval) {
    this.firstApproval = firstApproval;
  }

  public ApprovalHistory getSecondApproval() {
    return secondApproval;
  }

  public void setSecondApproval(ApprovalHistory secondApproval) {
    this.secondApproval = secondApproval;
  }
}