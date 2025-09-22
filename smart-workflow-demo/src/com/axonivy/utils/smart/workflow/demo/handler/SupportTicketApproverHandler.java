package com.axonivy.utils.smart.workflow.demo.handler;

import com.axonivy.utils.smart.workflow.demo.dto.Department;
import com.axonivy.utils.smart.workflow.demo.dto.Employee;
import com.axonivy.utils.smart.workflow.demo.dto.SupportTicket;
import com.axonivy.utils.smart.workflow.demo.service.DepartmentService;
import com.axonivy.utils.smart.workflow.demo.service.EmployeeService;

public final class SupportTicketApproverHandler {

  public static void chooseApprovers(SupportTicket ticket) {
    Employee employee = EmployeeService.getInstance().findByUsername(ticket.getEmployeeUsername());
    Department dept = DepartmentService.getInstance().findAll().stream()
        .filter(d -> d.getId().equals(employee.getDepartmentId())).findFirst().orElse(null);
    if (dept == null) {
      return;
    }

    dept.setFirstLevelManagerEmp(EmployeeService.getInstance().findByUsername(dept.getFirstLevelManager()));
    dept.setSecondLevelManagerEmp(EmployeeService.getInstance().findByUsername(dept.getSecondLevelManager()));

    switch (ticket.getType()) {
      case CUSTOMER -> chooseApproverForCustomerTicket(ticket, employee, dept);
      case HR -> chooseApproverForHrTicket(ticket, employee, dept);
      case OTHER -> chooseApproverForOtherTicket(ticket, employee, dept);
      case TECHNICAL -> chooseApproverForTechnicalTicket(ticket, employee, dept);
      default -> {}
    }
  }

  /**
   * For ticket type Customer
   * 1st level approver: Department manager
   * 2nd level approver: role SupportTeam
   * 
   * @param ticket
   * @param employee
   * @param dept
   */
  private static void chooseApproverForCustomerTicket(SupportTicket ticket, Employee employee, Department dept) {
    ticket.setFirstApprover("#" + dept.getSecondLevelManager());
    ticket.setSecondApprover("SupportTeam");
  }

  /**
   * For ticket type HR
   * 1st level approver: Department deputy manager
   * 2nd level approver: Department manager
   * 
   * @param ticket
   * @param employee
   * @param dept
   */
  private static void chooseApproverForHrTicket(SupportTicket ticket, Employee employee, Department dept) {
    ticket.setFirstApprover("#" + dept.getFirstLevelManager());
    ticket.setSecondApprover("#" + dept.getSecondLevelManager());
  }

  /**
   * For ticket type Other
   * 1st level approver: Department deputy manager
   * 2nd level approver: Department manager
   * 
   * @param ticket
   * @param employee
   * @param dept
   */
  private static void chooseApproverForOtherTicket(SupportTicket ticket, Employee employee, Department dept) {
    ticket.setFirstApprover("#" + dept.getFirstLevelManager());
    ticket.setSecondApprover("#" + dept.getSecondLevelManager());
  }

  /**
   * For ticket type Technical
   * 1st level approver: Department manager
   * 2nd level approver: Role TechnicalTeam
   * 
   * @param ticket
   * @param employee
   * @param dept
   */
  private static void chooseApproverForTechnicalTicket(SupportTicket ticket, Employee employee, Department dept) {
    ticket.setFirstApprover("#" + dept.getSecondLevelManager());
    ticket.setSecondApprover("TechnicalTeam");
  }
}
