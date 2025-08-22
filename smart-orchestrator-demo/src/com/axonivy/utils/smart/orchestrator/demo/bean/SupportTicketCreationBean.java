package com.axonivy.utils.smart.orchestrator.demo.bean;

import java.io.Serializable;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import com.axonivy.utils.smart.orchestrator.demo.dto.Employee;
import com.axonivy.utils.smart.orchestrator.demo.service.EmployeeService;

@ManagedBean
@ViewScoped
public class SupportTicketCreationBean implements Serializable {

  private static final long serialVersionUID = -3552762981245325219L;

  private Employee employee;
  private String content;

  public void init(String username) {
    employee = EmployeeService.getInstance().findByUsername(username);
  }

  public void startAgent() {

  }

  public Employee getEmployee() {
    return employee;
  }

  public void setEmployee(Employee employee) {
    this.employee = employee;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }
}
