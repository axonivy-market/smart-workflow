package com.axonivy.utils.smart.workflow.demo.employee;

import java.util.List;

import ch.ivyteam.ivy.environment.Ivy;

public class EmployeeRepository {

  private static final String FIELD_USERNAME = "username";

  private static EmployeeRepository instance;

  public static EmployeeRepository getInstance() {
    if (instance == null) {
      instance = new EmployeeRepository();
    }
    return instance;
  }

  public Employee create(Employee employee) {
    if (employee == null) {
      throw new IllegalArgumentException("Employee cannot be null");
    }
    Ivy.repo().save(employee);
    return employee;
  }

  public List<Employee> findAll() {
    return Ivy.repo().search(Employee.class).execute().getAll();
  }

  public Employee findByUsername(String username) {
    return Ivy.repo().search(Employee.class).textField(FIELD_USERNAME).isEqualToIgnoringCase(username).execute()
        .getFirst();
  }
}
