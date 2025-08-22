package com.axonivy.utils.smart.orchestrator.demo.service;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.smart.orchestrator.demo.dto.Employee;
import com.axonivy.utils.smart.orchestrator.utils.JsonUtils;

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
    if (StringUtils.isBlank(username)) {
      return null;
    }

    String query = username.startsWith("#") ? username.substring(1) : username;

    List<Employee> employees = JsonUtils.jsonValueToEntities(Ivy.var().get(VARIABLE_KEY), Employee.class);
    Employee found = employees.stream().filter(e -> e.getUsername().equals(query)).findFirst().get();

    if (StringUtils.isNotBlank(found.getDepartmentId())) {
      found.setDepartment(DepartmentService.getInstance().findAll().stream()
          .filter(dept -> dept.getId().equals(found.getDepartmentId())).findFirst().orElse(null));
    }

    return found;
  }
}
