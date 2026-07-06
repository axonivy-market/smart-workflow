package com.axonivy.utils.smart.workflow.demo.document.enums;

import java.util.Arrays;

public enum LegalDocumentType {

  CERTIFICATION("Certification", null, null, false, true),
  BANKING_CONFIRMATION("Banking Confirmation", null, null, false, false),
  CONTRACT("Contract", null, null, false, false),
  COMMERCIAL_REGISTER("Commercial Register", null, null, true, false),
  TAX_CERTIFICATE("Tax Certificate", null, null, false, false),
  SELF_DECLARATION("Self Declaration", null, null, true, false),
  ANNUAL_REPORT("Annual Report", null, null, true, false),
  OTHER("Other", null, null, false, false),

  ISO_9001("ISO 9001 — Quality Management",
      "Required for suppliers > €50k annual volume", "Upload certificate", true, true),
  ISO_14001("ISO 14001 — Environmental Management",
      "Required for manufacturing suppliers", "Upload certificate", true, true),
  ISO_27001("ISO 27001 — Information Security",
      "Required for IT / data processing suppliers", "Upload certificate", true, true),
  GDPR_DPA("GDPR Data Processing Agreement",
      "Required if supplier processes personal data", "Upload DPA", true, true);

  private final String label;
  private final String subtitle;
  private final String uploadLabel;
  private final boolean required;
  private final boolean certification;

  LegalDocumentType(String label, String subtitle, String uploadLabel,
      boolean required, boolean certification) {
    this.label = label;
    this.subtitle = subtitle;
    this.uploadLabel = uploadLabel;
    this.required = required;
    this.certification = certification;
  }

  public String getLabel() {
    return label;
  }

  public String getSubtitle() {
    return subtitle;
  }

  public String getUploadLabel() {
    return uploadLabel;
  }

  public boolean isRequired() {
    return required;
  }

  public boolean isCertification() {
    return certification;
  }

  public String getDocumentTypeKey() {
    if (certification && this != CERTIFICATION) {
      return "CERTIFICATION:" + name();
    }
    return name();
  }

  public static LegalDocumentType[] certificationValues() {
    return Arrays.stream(values())
        .filter(t -> t.isCertification() && t != CERTIFICATION)
        .toArray(LegalDocumentType[]::new);
  }

  public static LegalDocumentType certificationFromFileName(String fileName) {
    if (fileName == null) {
      return null;
    }
    String lower = fileName.toLowerCase();
    if (lower.contains("9001")) {
      return ISO_9001;
    }
    if (lower.contains("14001")) {
      return ISO_14001;
    }
    if (lower.contains("27001")) {
      return ISO_27001;
    }
    if (lower.contains("gdpr") || lower.contains("dpa")) {
      return GDPR_DPA;
    }
    return null;
  }

  public static LegalDocumentType fromFileName(String fileName) {
    if (fileName == null) {
      return OTHER;
    }
    String lower = fileName.toLowerCase();
    if (lower.contains("9001")) {
      return ISO_9001;
    }
    if (lower.contains("14001")) {
      return ISO_14001;
    }
    if (lower.contains("27001")) {
      return ISO_27001;
    }
    if (lower.contains("gdpr") || lower.contains("dpa")) {
      return GDPR_DPA;
    }
    if (lower.contains("cert") || lower.contains("iso")) {
      return CERTIFICATION;
    }
    if (lower.contains("bank") || lower.contains("iban") || lower.contains("bic")) {
      return BANKING_CONFIRMATION;
    }
    if (lower.contains("contract") || lower.contains("agreement")) {
      return CONTRACT;
    }
    if (lower.contains("register") || lower.contains("hrb") || lower.contains("commercial")) {
      return COMMERCIAL_REGISTER;
    }
    if (lower.contains("tax") || lower.contains("vat") || lower.contains("ust")) {
      return TAX_CERTIFICATE;
    }
    if (lower.contains("decl") || lower.contains("declaration")) {
      return SELF_DECLARATION;
    }
    if (lower.contains("annual") || lower.contains("report")) {
      return ANNUAL_REPORT;
    }
    return OTHER;
  }
}
