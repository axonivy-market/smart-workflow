package com.axonivy.utils.ai.axon.ivy.ai.demo.dto;

import java.io.Serializable;

import com.axonivy.utils.ai.axon.ivy.ai.demo.enums.Position;
import com.fasterxml.jackson.annotation.JsonIgnore;

public class Employee implements Serializable {

  private static final long serialVersionUID = 8036880473086829722L;

  private String username;
  private String fullName;
  private Position position;
  private String departmentId;
  private Integer maxLeaveDays;
  private Integer usedLeaveDays;
  private String email;

  @JsonIgnore
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
