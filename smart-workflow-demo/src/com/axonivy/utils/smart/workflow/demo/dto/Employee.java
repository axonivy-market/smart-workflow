package com.axonivy.utils.smart.workflow.demo.dto;

import java.io.Serializable;

import com.axonivy.utils.smart.workflow.demo.enums.Position;
import com.fasterxml.jackson.annotation.JsonIgnore;

import dev.langchain4j.model.output.structured.Description;

public class Employee implements Serializable {

  private static final long serialVersionUID = 8036880473086829722L;

  @Description("Unique username of the employee")
  private String username;

  @Description("Full legal name of the employee")
  private String fullName;

  @Description("Position or job title of the employee within the organization")
  private Position position;

  @Description("Identifier of the department the employee belongs to")
  private String departmentId;

  @Description("Maximum number of leave days allocated to the employee per year")
  private Integer maxLeaveDays;

  @Description("Number of leave days already used by the employee")
  private Integer usedLeaveDays;

  @Description("Work email address of the employee")
  private String email;

  @JsonIgnore
  @Description("Department details of the employee, ignored during JSON serialization")
  private Department department;

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getFullName() {
    return fullName;
  }

  public void setFullName(String fullName) {
    this.fullName = fullName;
  }

  public Position getPosition() {
    return position;
  }

  public void setPosition(Position position) {
    this.position = position;
  }

  public Department getDepartment() {
    return department;
  }

  public void setDepartment(Department department) {
    this.department = department;
  }

  public Integer getMaxLeaveDays() {
    return maxLeaveDays;
  }

  public void setMaxLeaveDays(Integer maxLeaveDays) {
    this.maxLeaveDays = maxLeaveDays;
  }

  public Integer getUsedLeaveDays() {
    return usedLeaveDays;
  }

  public void setUsedLeaveDays(Integer usedLeaveDays) {
    this.usedLeaveDays = usedLeaveDays;
  }

  public String getDepartmentId() {
    return departmentId;
  }

  public void setDepartmentId(String departmentId) {
    this.departmentId = departmentId;
  }

  public Integer getRemainingDays() {
    return maxLeaveDays - usedLeaveDays;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }
}
