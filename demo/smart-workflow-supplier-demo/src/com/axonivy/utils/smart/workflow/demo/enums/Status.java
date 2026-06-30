package com.axonivy.utils.smart.workflow.demo.enums;

public enum Status {
  SUCCESS("text-green-600", "ti-circle-check"),
  ERROR  ("text-red-600",   "ti-circle-x");

  public final String colorClass;
  public final String iconClass;

  Status(String colorClass, String iconClass) {
    this.colorClass = colorClass;
    this.iconClass  = iconClass;
  }
}