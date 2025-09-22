package com.axonivy.utils.smart.workflow.demo.dto;

import com.axonivy.utils.smart.workflow.demo.enums.TicketType;
import com.axonivy.utils.smart.workflow.utils.IdGenerationUtils;
import com.fasterxml.jackson.annotation.JsonIgnore;

import dev.langchain4j.model.output.structured.Description;

public class SupportTicket {

  private String id;

  @Description("Type of the support ticket. It's an enum field")
  private TicketType type;
  @Description("Name of the support ticket. Must be start with prefix 'Support ticket: '")
  private String name;
  @Description("description of the support ticket")
  private String description;
  @Description("username of the employee requested this ticket")
  private String employeeUsername;
  @Description("username of the first approver of this ticket. Field type: approval")
  private String firstApprover;
  @Description("username of the second approver of this ticket. Field type: approval")
  private String secondApprover;
  @Description("approval decision by AI. Field type: approval")
  private AiApprovalDecision aiApproval;
  @Description("approval decision by the first approver. Field type: approval")
  private ApprovalHistory firstApproval;
  @Description("approval decision by the second approver. Field type: approval")
  private ApprovalHistory secondApproval;
  @Description("The request date of this support ticket. Do not fill this field")
  private String requestedDate;


  @JsonIgnore
  private Employee requestor;

  public SupportTicket() {
    setId(IdGenerationUtils.generateRandomId());
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

  public String getRequestedDate() {
    return requestedDate;
  }

  public void setRequestedDate(String requestedDate) {
    this.requestedDate = requestedDate;
  }

  public AiApprovalDecision getAiApproval() {
    return aiApproval;
  }

  public void setAiApproval(AiApprovalDecision aiApproval) {
    this.aiApproval = aiApproval;
  }
}