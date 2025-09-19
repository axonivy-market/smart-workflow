package com.axonivy.utils.smart.orchestrator.demo.shopping.supplier;

import com.axonivy.utils.smart.orchestrator.demo.shopping.common.Address;

import dev.langchain4j.model.output.structured.Description;

public class Supplier {

  @Description("Unique supplier identifier")
  private String supplierId;

  @Description("Legal business name of the supplier")
  private String businessName;

  @Description("Primary business address")
  private Address businessAddress;

  @Description("Main phone number")
  private String phone;

  @Description("Main email address")
  private String email;

  @Description("Company website URL")
  private String website;

  public String getSupplierId() {
    return supplierId;
  }

  public void setSupplierId(String supplierId) {
    this.supplierId = supplierId;
  }

  public String getBusinessName() {
    return businessName;
  }

  public void setBusinessName(String businessName) {
    this.businessName = businessName;
  }

  public Address getBusinessAddress() {
    return businessAddress;
  }

  public void setBusinessAddress(Address businessAddress) {
    this.businessAddress = businessAddress;
  }

  public String getPhone() {
    return phone;
  }

  public void setPhone(String phone) {
    this.phone = phone;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getWebsite() {
    return website;
  }

  public void setWebsite(String website) {
    this.website = website;
  }
}