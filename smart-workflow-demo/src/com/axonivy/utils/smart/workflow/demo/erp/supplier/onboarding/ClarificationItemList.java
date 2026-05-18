package com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding;

import java.util.List;

import dev.langchain4j.model.output.structured.Description;

@Description("A list of classified clarification items for a supplier onboarding case")
public class ClarificationItemList {

  @Description("The classified clarification items. Each item must include all fields: message, problemType, documentTypeKey (if applicable), explanation (if applicable), and resolved=false.")
  private List<ClarificationItem> items;

  public List<ClarificationItem> getItems() {
    return items;
  }

  public void setItems(List<ClarificationItem> items) {
    this.items = items;
  }
}
