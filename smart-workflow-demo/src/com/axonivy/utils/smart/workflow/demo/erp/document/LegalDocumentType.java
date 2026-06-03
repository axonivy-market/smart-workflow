package com.axonivy.utils.smart.workflow.demo.erp.document;

import java.util.Arrays;

import dev.langchain4j.model.output.structured.Description;

@Description("Type of legal document. For certification sub-types use ISO_9001, ISO_14001, ISO_27001, or GDPR_DPA directly instead of CERTIFICATION.")
public enum LegalDocumentType {

  // ── General document types ──────────────────────────────────────────────────

  CERTIFICATION("Certification", null, null, false, true),
  BANKING_CONFIRMATION("Banking Confirmation", null, null, false, false),
  CONTRACT("Contract", null, null, false, false),
  COMMERCIAL_REGISTER("Commercial Register", null, null, true, false),
  TAX_CERTIFICATE("Tax Certificate", null, null, false, false),
  SELF_DECLARATION("Self Declaration", null, null, true, false),
  ANNUAL_REPORT("Annual Report", null, null, true, false),
  OTHER("Other", null, null, false, false),

  // ── Certification sub-types (merged from CertificationType) ────────────────

  ISO_9001("ISO 9001 — Quality Management",
      "Required for suppliers > €50k annual volume", "Upload certificate", true, true),
  ISO_14001("ISO 14001 — Environmental Management",
      "Required for manufacturing suppliers", "Upload certificate", true, true),
  ISO_27001("ISO 27001 — Information Security",
      "Required for IT / data processing suppliers", "Upload certificate", true, true),
  GDPR_DPA("GDPR Data Processing Agreement",
      "Required if supplier processes personal data", "Upload DPA", true, true);

  private final String label;
  private final String subtitle;    // non-null only for certification sub-types
  private final String uploadLabel; // non-null only for certification sub-types
  private final boolean required;
  private final boolean certification; // true for CERTIFICATION and all cert sub-types

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

  /** Returns {@code true} for {@link #CERTIFICATION} and all four specific cert sub-types. */
  public boolean isCertification() {
    return certification;
  }

  /**
   * Returns a key suitable for linking UI actions to this document type.
   * Specific cert sub-types return {@code "CERTIFICATION:<name>"} (e.g. {@code "CERTIFICATION:ISO_9001"}).
   * All other types return their {@link #name()} directly.
   */
  public String getDocumentTypeKey() {
    if (certification && this != CERTIFICATION) {
      return "CERTIFICATION:" + name();
    }
    return name();
  }

  /**
   * Returns only the four specific certification sub-types
   * (ISO_9001, ISO_14001, ISO_27001, GDPR_DPA), excluding the generic {@link #CERTIFICATION}.
   */
  public static LegalDocumentType[] certificationValues() {
    return Arrays.stream(values())
        .filter(t -> t.isCertification() && t != CERTIFICATION)
        .toArray(LegalDocumentType[]::new);
  }

  /**
   * Returns the certification sub-type whose name keywords appear in the given file name,
   * or {@code null} if no specific sub-type matches.
   */
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

  /**
   * Infers the document type from a file name.
   */
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
