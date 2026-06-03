package com.axonivy.utils.smart.workflow.demo.erp.supplier.model;

import dev.langchain4j.model.output.structured.Description;

public class SupplierContact {

  @Description("First name of the contact person")
  private String firstName;

  @Description("Last name of the contact person")
  private String lastName;

  @Description("Job title or role")
  private String jobTitle;

  @Description("Contact email address")
  private String email;

  @Description("Contact phone number")
  private String phone;

  public SupplierContact() {
  }

  public SupplierContact(String firstName, String lastName, String jobTitle, String email, String phone) {
    this.firstName = firstName;
    this.lastName = lastName;
    this.jobTitle = jobTitle;
    this.email = email;
    this.phone = phone;
  }

  public String getFirstName() {
    return firstName;
  }

  public void setFirstName(String firstName) {
    this.firstName = firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public void setLastName(String lastName) {
    this.lastName = lastName;
  }

  public String getJobTitle() {
    return jobTitle;
  }

  public void setJobTitle(String jobTitle) {
    this.jobTitle = jobTitle;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getPhone() {
    return phone;
  }

  public void setPhone(String phone) {
    this.phone = phone;
  }
}
