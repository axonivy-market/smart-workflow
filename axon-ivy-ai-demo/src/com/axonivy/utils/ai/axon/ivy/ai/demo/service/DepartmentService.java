package com.axonivy.utils.ai.axon.ivy.ai.demo.service;

import java.util.ArrayList;
import java.util.List;

import com.axonivy.utils.ai.axon.ivy.ai.demo.dto.Department;
import com.axonivy.utils.ai.utils.JsonUtils;

import ch.ivyteam.ivy.environment.Ivy;

public class DepartmentService {
  private static final String VARIABLE_KEY = "AiDemo.Department";

  private static DepartmentService instance;

  public static DepartmentService getInstance() {
    if (instance == null) {
      instance = new DepartmentService();
    }
    
    return instance;
  }

  public List<Department> findAll() {
    try {
      return JsonUtils.jsonValueToEntities(Ivy.var().get(VARIABLE_KEY), Department.class);
    } catch (Exception e) {
      return new ArrayList<>();
    }
  }
}
