package com.axonivy.utils.smart.workflow.demo.erp.mock;

import com.axonivy.utils.smart.workflow.demo.erp.department.model.Department;
import com.axonivy.utils.smart.workflow.demo.erp.department.repository.DepartmentRepository;

import ch.ivyteam.ivy.environment.Ivy;

public class MockDataGenerator {

  public static void ensureDepartments() {
    if (DepartmentRepository.getInstance().findAll().isEmpty()) {
      generateDepartments();
    }
  }

  public static void generateDepartments() {
    Ivy.log().info("Generating mock departments...");

    DepartmentRepository repo = DepartmentRepository.getInstance();
    repo.create(department("DEPT-001", "Lumber & Building Materials", "robert.hayes",   "sandra.collins"));
    repo.create(department("DEPT-002", "Electrical & Plumbing",       "karen.mitchell", "sandra.collins"));
    repo.create(department("DEPT-003", "Tools & Hardware",            "james.thornton", "sandra.collins"));
    repo.create(department("DEPT-004", "Flooring & Decor",            "lisa.nguyen",    "sandra.collins"));
    repo.create(department("DEPT-005", "Garden & Outdoor Living",     "marcus.webb",    "sandra.collins"));

    Ivy.log().info("Generated 5 departments for BuildRight Supply Co.");
  }

  private static Department department(String id, String name, String firstLevelManager, String secondLevelManager) {
    Department dept = new Department();
    dept.setId(id);
    dept.setName(name);
    dept.setFirstLevelManager(firstLevelManager);
    dept.setSecondLevelManager(secondLevelManager);
    return dept;
  }
}
