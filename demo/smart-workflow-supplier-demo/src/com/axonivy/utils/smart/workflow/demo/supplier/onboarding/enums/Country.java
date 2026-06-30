package com.axonivy.utils.smart.workflow.demo.supplier.onboarding.enums;

public enum Country {
  DE("DE", "Germany"),
  FR("FR", "France"),
  GB("GB", "United Kingdom"),
  CH("CH", "Switzerland"),
  US("US", "United States");

  private final String code;
  private final String displayName;

  Country(String code, String displayName) {
    this.code = code;
    this.displayName = displayName;
  }

  public String getCode() {
    return code;
  }

  public String getDisplayName() {
    return displayName;
  }
}
