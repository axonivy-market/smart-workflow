package com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding;

public enum Country {
  AT("AT", "Austria"),
  BE("BE", "Belgium"),
  BR("BR", "Brazil"),
  CA("CA", "Canada"),
  CN("CN", "China"),
  CZ("CZ", "Czech Republic"),
  DK("DK", "Denmark"),
  FI("FI", "Finland"),
  FR("FR", "France"),
  DE("DE", "Germany"),
  GR("GR", "Greece"),
  HU("HU", "Hungary"),
  IN("IN", "India"),
  ID("ID", "Indonesia"),
  IE("IE", "Ireland"),
  IT("IT", "Italy"),
  JP("JP", "Japan"),
  LU("LU", "Luxembourg"),
  MX("MX", "Mexico"),
  NL("NL", "Netherlands"),
  NZ("NZ", "New Zealand"),
  NO("NO", "Norway"),
  PL("PL", "Poland"),
  PT("PT", "Portugal"),
  RO("RO", "Romania"),
  SG("SG", "Singapore"),
  SK("SK", "Slovakia"),
  ZA("ZA", "South Africa"),
  KR("KR", "South Korea"),
  ES("ES", "Spain"),
  SE("SE", "Sweden"),
  CH("CH", "Switzerland"),
  TW("TW", "Taiwan"),
  TH("TH", "Thailand"),
  TR("TR", "Turkey"),
  GB("GB", "United Kingdom"),
  US("US", "United States"),
  VN("VN", "Vietnam");

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
