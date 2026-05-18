package com.axonivy.utils.smart.workflow.demo.erp.document;

public enum LegalDocumentObjectType {

  SUPPLIER("Supplier"),
  PRODUCT("Product");

  private final String label;

  LegalDocumentObjectType(String label) {
    this.label = label;
  }

  public String getLabel() {
    return label;
  }
}
