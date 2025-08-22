package com.axonivy.utils.smart.orchestrator.demo.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class Department {
  private String id;
  private String name;
  private String firstLevelManager;
  private String secondLevelManager;

  @JsonIgnore
  private Employee firstLevelManagerEmp;

  @JsonIgnore
  private Employee secondLevelManagerEmp;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getFirstLevelManager() {
    return firstLevelManager;
  }

  public void setFirstLevelManager(String firstLevelManager) {
    this.firstLevelManager = firstLevelManager;
  }

  public String getSecondLevelManager() {
    return secondLevelManager;
  }

  public void setSecondLevelManager(String secondLevelManager) {
    this.secondLevelManager = secondLevelManager;
  }

  public Employee getFirstLevelManagerEmp() {
    return firstLevelManagerEmp;
  }

  public void setFirstLevelManagerEmp(Employee firstLevelManagerEmp) {
    this.firstLevelManagerEmp = firstLevelManagerEmp;
  }

  public Employee getSecondLevelManagerEmp() {
    return secondLevelManagerEmp;
  }

  public void setSecondLevelManagerEmp(Employee secondLevelManagerEmp) {
    this.secondLevelManagerEmp = secondLevelManagerEmp;
  }
}
