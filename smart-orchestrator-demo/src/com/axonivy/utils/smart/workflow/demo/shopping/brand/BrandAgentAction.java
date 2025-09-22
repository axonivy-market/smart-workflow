package com.axonivy.utils.smart.workflow.demo.shopping.brand;

public enum BrandAgentAction {

  SEARCH("""
      1. Find exact brand using the given brand information
        1.1. If exact brand exist, skip step 2
        1.2. If exact brand doesn't exist, do step 2
      2. Find similiar brands
      3. Finally, summarize all the feedbacks
      """),
  CREATE(
      """
      1. Find the given brand
      2. If the brand doesn't exist, create it
      3. If the brand is existing, return it as result
      """);

  private String systemInstruction;

  private BrandAgentAction(String systemInstruction) {
    this.systemInstruction = systemInstruction;
  }

  public String getSystemInstruction() {
    return systemInstruction;
  }

  public void setSystemInstruction(String systemInstruction) {
    this.systemInstruction = systemInstruction;
  }

}