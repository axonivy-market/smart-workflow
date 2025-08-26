package com.axonivy.utils.smart.orchestrator.demo.dto;

import com.axonivy.utils.smart.orchestrator.demo.enums.AxonIvyTicketType;
import com.axonivy.utils.smart.orchestrator.utils.IdGenerationUtils;
import com.fasterxml.jackson.annotation.JsonIgnore;

import dev.langchain4j.model.output.structured.Description;

public class AxonIvySupportTicket {

  
  private String id;
  @Description("Type of the support ticket. It's an enum field")
  private AxonIvyTicketType type;
  @Description("Title of the support ticket. Must be start with prefix 'Axon Ivy Support: '")
  private String title;
  @Description("description of the support ticket")
  private String description;
  @Description("name of user who submits this ticket")
  private String reporter;
  private AiApprovalDecision aiApproval;
  private ApprovalHistory firstApproval;
  @Description("created date of the support ticket")
  private String requestedDate;
  @Description("version of the reported product. Example: ivy version, Portal version,...")
  private String version;
  @Description("system operating of the customer environment")
  private String systemOS;
  @Description("answer from AI")
  private String solution;


  @JsonIgnore
  private Employee requestor;

  public AxonIvySupportTicket() {
    setId(IdGenerationUtils.generateRandomId());
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public AxonIvyTicketType getType() {
    return type;
  }

  public void setType(AxonIvyTicketType type) {
    this.type = type;
  }

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

  public AiApprovalDecision getAiApproval() {
    return aiApproval;
  }

  public void setAiApproval(AiApprovalDecision aiApproval) {
    this.aiApproval = aiApproval;
  }

  public ApprovalHistory getFirstApproval() {
    return firstApproval;
  }

  public void setFirstApproval(ApprovalHistory firstApproval) {
    this.firstApproval = firstApproval;
  }

  public String getRequestedDate() {
    return requestedDate;
  }

  public void setRequestedDate(String requestedDate) {
    this.requestedDate = requestedDate;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public String getSystemOS() {
    return systemOS;
  }

  public void setSystemOS(String systemOS) {
    this.systemOS = systemOS;
  }

  public Employee getRequestor() {
    return requestor;
  }

  public void setRequestor(Employee requestor) {
    this.requestor = requestor;
  }

  public String getSolution() {
    return solution;
  }

  public void setSolution(String solution) {
    this.solution = solution;
  }

  public String getReporter() {
    return reporter;
  }

  public void setReporter(String reporter) {
    this.reporter = reporter;
  }
  
}