package com.axonivy.utils.smart.workflow.demo.erp.supplier.agent;

public enum SupplierAgentAction {

  SEARCH("""
      1. Find exact supplier using the given supplier information
        1.1. If exact supplier exist, skip step 2
        1.2. If exact supplier doesn't exist, do step 2
      2. Find similar suppliers
      3. Finally, summarize all the feedbacks
      """),
  CREATE(
      """
      1. Find the given supplier
      2. If the supplier doesn't exist, create it
      3. If the supplier is existing, return it as result
      """),
  DUPLICATE_CHECK(
      """
      1. Search the supplier database for exact and similar matches by name, country, and business purpose
      2. For each match found, calculate a similarity percentage
      3. Return all matches sorted by score, and indicate if an exact match exists
      """),
  VALIDATE(
      """
      1. Extract structured data from uploaded documents (certificates, registration)
      2. Scrape the supplier website for additional company information
      3. Validate all data against internal supplier policy using RAG
      4. Run sanctions and blacklist checks (EU, UN, OFAC)
      5. Return validation findings classified as PASSED, WARNING, or FAILURE
      """),
  RISK_SCORE(
      """
      1. Calculate component risk scores: financial stability, policy compliance, cert validity, sanctions compliance
      2. Compute the aggregate risk score (0-100)
      3. Determine the risk level: GREEN (>=70), YELLOW (40-69), RED (<40)
      4. Decide the routing decision: APPROVAL, CLARIFICATION, or DECLINE
      5. Document the full decision trail
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