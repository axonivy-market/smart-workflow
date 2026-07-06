package com.axonivy.utils.smart.workflow.demo.supplier.onboarding.enums;

public enum Urgency {
  NORMAL, HIGH, CRITICAL;

  public String getLabel() {
    return name().charAt(0) + name().substring(1).toLowerCase();
  }

  public static Urgency fromString(String value) {
    for (Urgency urgency : Urgency.values()) {
      if (urgency.name().equalsIgnoreCase(value)) {
        return urgency;
      }
    }
    return null;
  }
}
