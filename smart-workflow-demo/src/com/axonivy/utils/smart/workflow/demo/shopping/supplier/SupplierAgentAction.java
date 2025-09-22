package com.axonivy.utils.smart.workflow.demo.shopping.supplier;

public enum SupplierAgentAction {

  SEARCH("""
      1. Find exact supplier using the given supplier information
        1.1. If exact supplier exist, skip step 2
        1.2. If exact supplier doesn't exist, do step 2
      2. Find similiar suppliers
      3. Finally, summarize all the feedbacks
      """),
  CREATE(
      """
      1. Find the given supplier
      2. If the supplier doesn't exist, create it
      3. If the supplier is existing, return it as result
      """);

  private String systemInstruction;

  private SupplierAgentAction(String systemInstruction) {
    this.systemInstruction = systemInstruction;
  }

  public String getSystemInstruction() {
    return systemInstruction;
  }

  public void setSystemInstruction(String systemInstruction) {
    this.systemInstruction = systemInstruction;
  }

}