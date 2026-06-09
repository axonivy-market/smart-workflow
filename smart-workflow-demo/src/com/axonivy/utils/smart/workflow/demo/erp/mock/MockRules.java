package com.axonivy.utils.smart.workflow.demo.erp.mock;

import com.axonivy.utils.smart.workflow.demo.erp.document.LegalDocumentType;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.model.RuleType;

public enum MockRules {

  // ── Policy (compliance) rules ─────────────────────────────────────────────

  RULE_03_ISO_9001(RuleType.POLICY,
      "If ISO 9001 certificate is uploaded, validate that it is not expired and the certificate number is properly formatted. Flag as WARNING if the cert expires within 6 months.",
      20, LegalDocumentType.ISO_9001),
  RULE_04_ISO_14001(RuleType.POLICY,
      "If ISO 14001 certificate is uploaded, validate that it is not expired and the scope covers the supplier's stated manufacturing or industrial operations.",
      20, LegalDocumentType.ISO_14001),
  RULE_05_ISO_27001(RuleType.POLICY,
      "If ISO 27001 certificate is uploaded, validate that it is not expired and the scope covers IT or data processing activities relevant to the supplier.",
      20, LegalDocumentType.ISO_27001),
  RULE_06_GDPR_DPA(RuleType.POLICY,
      "If a GDPR Data Processing Agreement is uploaded, validate that it covers the supplier's data processing obligations and appears properly signed or executed.",
      20, LegalDocumentType.GDPR_DPA),
  RULE_07_EU_EEA_PREFERENCE(RuleType.POLICY,
      "Suppliers outside EU/EEA should be flagged as warning.",
      10, null),
  RULE_08_ANNUAL_REPORT_FINANCIAL_SOUNDNESS(RuleType.POLICY,
      "Annual report is recommended; if present, financial soundness checks must pass.",
      15, LegalDocumentType.ANNUAL_REPORT),

  // ── Financial rules ───────────────────────────────────────────────────────

  FIN_RULE_03_NEGATIVE_EQUITY(RuleType.FINANCIAL,
      "If the annual report indicates negative total equity or net liabilities exceeding total assets, flag as FAILURE.",
      40, LegalDocumentType.ANNUAL_REPORT),
  FIN_RULE_04_OPERATING_LOSS(RuleType.FINANCIAL,
      "If the annual report shows an operating loss for the reported fiscal year, flag as WARNING. Do not consider the report date or whether the report is current — only evaluate the financial content present in the document.",
      20, LegalDocumentType.ANNUAL_REPORT),
  FIN_RULE_05_INSOLVENCY_PROCEEDINGS(RuleType.FINANCIAL,
      "If the annual report or any submitted document mentions insolvency proceedings, administration, receivership, or a court-ordered asset freeze, flag as FAILURE.",
      60, LegalDocumentType.ANNUAL_REPORT),
  FIN_RULE_06_COMPANY_DISSOLVED(RuleType.FINANCIAL,
      "If the commercial register extract shows the company as dissolved, struck off, or in liquidation, flag as FAILURE.",
      80, LegalDocumentType.COMMERCIAL_REGISTER),
  FIN_RULE_07_TAX_CERTIFICATE_EXPIRED(RuleType.FINANCIAL,
      "If a tax certificate is uploaded and its validity date has passed, flag as WARNING. An expired tax certificate indicates unresolved compliance obligations.",
      15, LegalDocumentType.TAX_CERTIFICATE),
  FIN_RULE_08_BANKING_ACCOUNT_MISMATCH(RuleType.FINANCIAL,
      "If a banking confirmation is uploaded and the account holder name does not match the registered supplier legal name, flag as WARNING.",
      20, LegalDocumentType.BANKING_CONFIRMATION),

  // ── Cert validity rules ───────────────────────────────────────────────────

  CERT_01_COMMERCIAL_REGISTER(RuleType.CERT_VALIDITY,
      "Deduct score if the commercial register extract is missing.",
      30, LegalDocumentType.COMMERCIAL_REGISTER),
  CERT_02_SELF_DECLARATION(RuleType.CERT_VALIDITY,
      "Deduct score if the supplier self-declaration is missing.",
      15, LegalDocumentType.SELF_DECLARATION),
  CERT_03_ANNUAL_REPORT(RuleType.CERT_VALIDITY,
      "Deduct score if the annual report is missing.",
      15, LegalDocumentType.ANNUAL_REPORT),
  CERT_04_CERTIFICATION_REQUIRED(RuleType.CERT_VALIDITY,
      "Deduct score if certification is missing for suppliers above the volume threshold.",
      30, LegalDocumentType.CERTIFICATION),
  CERT_05_CERT_EXPIRED(RuleType.CERT_VALIDITY,
      "Deduct score per expired certificate found in findings.",
      40, null),
  CERT_06_CERT_EXPIRING_SOON(RuleType.CERT_VALIDITY,
      "Deduct score per certificate expiring within 6 months.",
      10, null);

  private final RuleType ruleType;
  private final String rule;
  private final int riskScore;
  private final LegalDocumentType docType;

  MockRules(RuleType ruleType, String rule, int riskScore, LegalDocumentType docType) {
    this.ruleType = ruleType;
    this.rule = rule;
    this.riskScore = riskScore;
    this.docType = docType;
  }

  public RuleType ruleType() {
    return ruleType;
  }

  public String rule() {
    return rule;
  }

  public int riskScore() {
    return riskScore;
  }

  public LegalDocumentType docType() {
    return docType;
  }
}
