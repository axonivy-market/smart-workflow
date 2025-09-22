package com.axonivy.utils.smart.workflow.demo.enums;

public enum TicketType {
  TECHNICAL(2), CUSTOMER(3), HR(4), OTHER(5);

  private TicketType(int index) {
    this.index = index;
  }

  private int index;

  public int getIndex() {
    return this.index;
  }
}
