package com.axonivy.utils.smart.workflow.demo.erp.supplier.model;

import java.util.ArrayList;
import java.util.List;

import com.axonivy.utils.smart.workflow.demo.erp.shared.Address;

import dev.langchain4j.model.output.structured.Description;

public class Supplier {

  @Description("Unique supplier identifier")
  private String supplierId;

  @Description("Legal business name of the supplier")
  private String businessName;

  @Description("Legal form, e.g. GmbH, AG, SE")
  private String legalForm;

  @Description("VAT identification number, e.g. DE123456789")
  private String vatId;

  @Description("Commercial register number, e.g. HRB 123456")
  private String commercialRegisterNo;

  @Description("Business purpose or category, e.g. Logistics / Transport")
  private String businessPurpose;

  @Description("Primary business address")
  private Address businessAddress;

  @Description("Main phone number")
  private String phone;

  @Description("Main email address")
  private String email;

  @Description("Company website URL")
  private String website;

  @Description("Primary contact person at the supplier")
  private SupplierContact primaryContact;

  @Description("Banking and payment information")
  private SupplierBanking banking;

  @Description("List of certifications and compliance documents")
  private List<SupplierCertification> certifications;

  private List<String> requiredDocumentIds = new ArrayList<>();

  private List<String> certificationDocumentIds = new ArrayList<>();

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

  public String getLegalForm() {
    return legalForm;
  }

  public void setLegalForm(String legalForm) {
    this.legalForm = legalForm;
  }

  public String getVatId() {
    return vatId;
  }

  public void setVatId(String vatId) {
    this.vatId = vatId;
  }

  public String getCommercialRegisterNo() {
    return commercialRegisterNo;
  }

  public void setCommercialRegisterNo(String commercialRegisterNo) {
    this.commercialRegisterNo = commercialRegisterNo;
  }

  public String getBusinessPurpose() {
    return businessPurpose;
  }

  public void setBusinessPurpose(String businessPurpose) {
    this.businessPurpose = businessPurpose;
  }

  public SupplierContact getPrimaryContact() {
    return primaryContact;
  }

  public void setPrimaryContact(SupplierContact primaryContact) {
    this.primaryContact = primaryContact;
  }

  public SupplierBanking getBanking() {
    return banking;
  }

  public void setBanking(SupplierBanking banking) {
    this.banking = banking;
  }

  public List<SupplierCertification> getCertifications() {
    return certifications;
  }

  public void setCertifications(List<SupplierCertification> certifications) {
    this.certifications = certifications;
  }

  public List<String> getRequiredDocumentIds() {
    return requiredDocumentIds;
  }

  public void setRequiredDocumentIds(List<String> requiredDocumentIds) {
    this.requiredDocumentIds = requiredDocumentIds;
  }

  public List<String> getCertificationDocumentIds() {
    return certificationDocumentIds;
  }

  public void setCertificationDocumentIds(List<String> certificationDocumentIds) {
    this.certificationDocumentIds = certificationDocumentIds;
  }
}