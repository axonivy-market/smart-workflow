package com.axonivy.utils.smart.workflow.demo.erp.supplier.repository;

import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.smart.workflow.demo.erp.shared.Address;

import dev.langchain4j.internal.Json;
import dev.langchain4j.model.output.structured.Description;

public class SupplierSearchCriteria {

  @Description("Unique supplier identifier. Leave null if no filter is needed.")
  private String supplierId;

  @Description("Business name filter (case-insensitive partial match). Leave null if not filtering by business name.")
  private String businessNameContains;

  @Description("Primary business address. Leave null if not filtering by address.")
  private Address businessAddress;

  @Description("Main phone number filter (exact match). Leave null if not filtering by phone.")
  private String phone;

  @Description("Email filter (exact match). Leave null if not filtering by email.")
  private String email;

  @Description("Website URL filter (exact match). Leave null if not filtering by website.")
  private String website;

  @Description("VAT ID filter (exact match). Leave null if not filtering by VAT ID.")
  private String vatId;

  @Description("Business purpose or category filter (partial match). Leave null if not filtering by purpose.")
  private String businessPurposeContains;

  @Description("Country filter (exact match). Leave null if not filtering by country.")
  private String country;

  public SupplierSearchCriteria() {
  }

  // Getters and setters
  public String getSupplierId() {
    return supplierId;
  }

  public void setSupplierId(String supplierId) {
    this.supplierId = supplierId;
  }

  public String getBusinessNameContains() {
    return businessNameContains;
  }

  public void setBusinessNameContains(String businessNameContains) {
    this.businessNameContains = businessNameContains;
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

  /**
   * Checks if this search criteria has any filters set.
   */
  public String getVatId() {
    return vatId;
  }

  public void setVatId(String vatId) {
    this.vatId = vatId;
  }

  public String getBusinessPurposeContains() {
    return businessPurposeContains;
  }

  public void setBusinessPurposeContains(String businessPurposeContains) {
    this.businessPurposeContains = businessPurposeContains;
  }

  public String getCountry() {
    return country;
  }

  public void setCountry(String country) {
    this.country = country;
  }

  public boolean hasAnyFilter() {
    return StringUtils.isNotBlank(supplierId) || StringUtils.isNotBlank(businessNameContains) || businessAddress != null
        || StringUtils.isNotBlank(phone) || StringUtils.isNotBlank(email) || StringUtils.isNotBlank(website)
        || StringUtils.isNotBlank(vatId) || StringUtils.isNotBlank(businessPurposeContains)
        || StringUtils.isNotBlank(country);
  }

  @Override
  public String toString() {
    return Json.toJson(this);
  }
}