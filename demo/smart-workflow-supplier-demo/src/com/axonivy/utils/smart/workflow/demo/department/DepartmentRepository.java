package com.axonivy.utils.smart.workflow.demo.department;

import java.util.ArrayList;
import java.util.List;

import com.axonivy.utils.smart.workflow.demo.AbstractMockRepository;
import com.fasterxml.jackson.core.type.TypeReference;

public class DepartmentRepository extends AbstractMockRepository<Department> {

  private static final String FIELD = "MOCK_DEPARTMENTS";
  private static final TypeReference<List<Department>> LIST_TYPE = new TypeReference<List<Department>>() {};

  private static DepartmentRepository instance;

  public static DepartmentRepository getInstance() {
    if (instance == null) {
      instance = new DepartmentRepository();
    }
    return instance;
  }

  @Override
  protected String getField() {
    return FIELD;
  }

  @Override
  protected TypeReference<List<Department>> getListType() {
    return LIST_TYPE;
  }

  @Override
  protected List<Department> createMockData() {
    List<Department> list = new ArrayList<>();
    list.add(department("DEPT-001", "Lumber & Building Materials", "robert.hayes",   "sandra.collins"));
    list.add(department("DEPT-002", "Electrical & Plumbing",       "karen.mitchell", "sandra.collins"));
    list.add(department("DEPT-003", "Tools & Hardware",            "james.thornton", "sandra.collins"));
    list.add(department("DEPT-004", "Flooring & Decor",            "lisa.nguyen",    "sandra.collins"));
    list.add(department("DEPT-005", "Garden & Outdoor Living",     "marcus.webb",    "sandra.collins"));
    return list;
  }

  public Department findById(String caseUuid, String id) {
    return findAll(caseUuid).stream()
        .filter(d -> id.equalsIgnoreCase(d.getId()))
        .findFirst()
        .orElse(null);
  }

  public Department create(String caseUuid, Department department) {
    if (department == null) {
      throw new IllegalArgumentException("Department cannot be null");
    }
    List<Department> list = new ArrayList<>(findAll(caseUuid));
    list.add(department);
    save(caseUuid, list);
    return department;
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
