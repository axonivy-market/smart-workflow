package com.axonivy.utils.smart.workflow.demo.erp.department.model;

import java.io.Serializable;

public class Department implements Serializable {

  private static final long serialVersionUID = 1L;

  private String id;
  private String name;
  private String firstLevelManager;
  private String secondLevelManager;

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
}
