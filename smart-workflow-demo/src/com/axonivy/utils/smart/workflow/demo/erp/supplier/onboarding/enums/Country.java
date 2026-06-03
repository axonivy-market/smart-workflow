package com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.enums;

public enum Country {
  US("US", "United States"),
  CN("CN", "China"),
  DE("DE", "Germany"),
  GB("GB", "United Kingdom"),
  JP("JP", "Japan"),
  FR("FR", "France"),
  IN("IN", "India"),
  CA("CA", "Canada"),
  BR("BR", "Brazil"),
  KR("KR", "South Korea");

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
