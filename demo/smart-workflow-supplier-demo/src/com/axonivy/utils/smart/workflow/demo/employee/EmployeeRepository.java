package com.axonivy.utils.smart.workflow.demo.employee;

import java.util.ArrayList;
import java.util.List;

import com.axonivy.utils.smart.workflow.demo.AbstractMockRepository;
import com.fasterxml.jackson.core.type.TypeReference;

public class EmployeeRepository extends AbstractMockRepository<Employee> {

  private static final String FIELD = "MOCK_EMPLOYEES";
  private static final TypeReference<List<Employee>> LIST_TYPE = new TypeReference<List<Employee>>() {};

  private static EmployeeRepository instance;

  public static EmployeeRepository getInstance() {
    if (instance == null) {
      instance = new EmployeeRepository();
    }
    return instance;
  }

  @Override
  protected String getField() {
    return FIELD;
  }

  @Override
  protected TypeReference<List<Employee>> getListType() {
    return LIST_TYPE;
  }

  @Override
  protected List<Employee> createMockData() {
    List<Employee> list = new ArrayList<>();
    list.add(employee("sandra.collins", "Sandra", "Collins", null,       "ProcurementDirector", "sandra.collins@supplierco.com"));
    list.add(employee("robert.hayes",   "Robert", "Hayes",   "DEPT-001", "DepartmentManager",   "robert.hayes@supplierco.com"));
    list.add(employee("karen.mitchell", "Karen",  "Mitchell","DEPT-002", "DepartmentManager",   "karen.mitchell@supplierco.com"));
    list.add(employee("james.thornton", "James",  "Thornton","DEPT-003", "DepartmentManager",   "james.thornton@supplierco.com"));
    list.add(employee("lisa.nguyen",    "Lisa",   "Nguyen",  "DEPT-004", "DepartmentManager",   "lisa.nguyen@supplierco.com"));
    list.add(employee("marcus.webb",    "Marcus", "Webb",    "DEPT-005", "DepartmentManager",   "marcus.webb@supplierco.com"));
    list.add(employee("david.chen",     "David",  "Chen",    "DEPT-001", "Procurement",         "david.chen@supplierco.com"));
    list.add(employee("emily.ross",     "Emily",  "Ross",    "DEPT-002", "Procurement",         "emily.ross@supplierco.com"));
    list.add(employee("tom.banks",      "Tom",    "Banks",   "DEPT-003", "Procurement",         "tom.banks@supplierco.com"));
    list.add(employee("claire.ford",    "Claire", "Ford",    "DEPT-004", "Procurement",         "claire.ford@supplierco.com"));
    return list;
  }

  public Employee findByUsername(String caseUuid, String username) {
    return findAll(caseUuid).stream()
        .filter(e -> username.equalsIgnoreCase(e.getUsername()))
        .findFirst()
        .orElse(null);
  }

  public Employee create(String caseUuid, Employee employee) {
    if (employee == null) {
      throw new IllegalArgumentException("Employee cannot be null");
    }
    List<Employee> list = new ArrayList<>(findAll(caseUuid));
    list.add(employee);
    save(caseUuid, list);
    return employee;
  }

  private static Employee employee(String username, String firstName, String lastName,
      String departmentId, String role, String email) {
    Employee emp = new Employee();
    emp.setUsername(username);
    emp.setFirstName(firstName);
    emp.setLastName(lastName);
    emp.setDepartmentId(departmentId);
    emp.setRole(role);
    emp.setEmail(email);
    return emp;
  }
}
