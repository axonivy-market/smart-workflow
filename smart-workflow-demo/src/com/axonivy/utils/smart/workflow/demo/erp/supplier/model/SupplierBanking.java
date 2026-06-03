package com.axonivy.utils.smart.workflow.demo.erp.supplier.model;

import dev.langchain4j.model.output.structured.Description;

public class SupplierBanking {

  @Description("International Bank Account Number")
  private String iban;

  @Description("Bank Identifier Code / SWIFT")
  private String bic;

  @Description("Name of the bank")
  private String bankName;

  public SupplierBanking() {
  }

  public SupplierBanking(String iban, String bic, String bankName) {
    this.iban = iban;
    this.bic = bic;
    this.bankName = bankName;
  }

  public String getIban() {
    return iban;
  }

  public void setIban(String iban) {
    this.iban = iban;
  }

  public String getBic() {
    return bic;
  }

  public void setBic(String bic) {
    this.bic = bic;
  }

  public String getBankName() {
    return bankName;
  }

  public void setBankName(String bankName) {
    this.bankName = bankName;
  }
}
