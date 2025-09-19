package com.axonivy.utils.smart.orchestrator.demo.shopping.product;

public enum ProductAgentAction {
  SEARCH("""
      1. Find the given product
      """),
  CREATE("""
      1. Find the given product
      2. If the product doesn't exist, create it
      3. If the product is existing, return it as result
      """),
  DRY_RUN("""
      1. Find the given product using ALL information including supplier, category, brand, and other informations.
      1.1. If the product is found, finish, return the result, don't do other steps.
      1.2. Otherwise continue step 2
      2. Check dependencies of the given product
      3. Should return all details of the product even if it not exist or has errors.
      """),
  CREATE_CRITERIA("""
      1. ONLY run the tool to create search criteria.
      """);

  private String systemInstruction;

  private ProductAgentAction(String systemInstruction) {
    this.systemInstruction = systemInstruction;
  }

  public String getSystemInstruction() {
    return systemInstruction;
  }

  public void setSystemInstruction(String systemInstruction) {
    this.systemInstruction = systemInstruction;
  }

  public String getInstruction() {
    return this.systemInstruction;
  }
}