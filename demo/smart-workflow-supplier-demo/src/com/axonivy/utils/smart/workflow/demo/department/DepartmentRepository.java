package com.axonivy.utils.smart.workflow.demo.department;

import java.util.List;

import ch.ivyteam.ivy.environment.Ivy;

public class DepartmentRepository {

  private static final String FIELD_ID = "id";

  private static DepartmentRepository instance;

  public static DepartmentRepository getInstance() {
    if (instance == null) {
      instance = new DepartmentRepository();
    }
    return instance;
  }

  public Department create(Department department) {
    if (department == null) {
      throw new IllegalArgumentException("Department cannot be null");
    }
    Ivy.repo().save(department);
    return department;
  }

  public List<Department> findAll() {
    return Ivy.repo().search(Department.class).execute().getAll();
  }

  public Department findById(String id) {
    return Ivy.repo().search(Department.class).textField(FIELD_ID).isEqualToIgnoringCase(id).execute().getFirst();
  }
}
