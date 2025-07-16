package com.axonivy.utils.ai.axon.ivy.ai.demo.service;

import java.util.List;

import com.axonivy.utils.ai.axon.ivy.ai.demo.dto.Employee;
import com.axonivy.utils.ai.persistence.converter.BusinessEntityConverter;

import ch.ivyteam.ivy.environment.Ivy;

public class EmployeeService {
  private static final String VARIABLE_KEY = "AiDemo.Employee";

  private static EmployeeService instance;

  public static EmployeeService getInstance() {
    if (instance == null) {
      instance = new EmployeeService();
    }
    return instance;
  }

  public Employee findByUsername(String username) {
    List<Employee> employees = BusinessEntityConverter.jsonValueToEntities(Ivy.var().get(VARIABLE_KEY), Employee.class);
    return employees.stream().filter(e -> e.getUsername().equals(username)).findFirst().get();
  }
}
