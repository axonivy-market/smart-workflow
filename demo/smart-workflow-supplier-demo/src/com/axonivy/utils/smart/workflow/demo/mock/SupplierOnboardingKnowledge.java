package com.axonivy.utils.smart.workflow.demo.mock;

import java.util.List;

public final class SupplierOnboardingKnowledge {

  public static final String COLLECTION = "supplier-onboarding-demo";

  private SupplierOnboardingKnowledge() {}

  // ── 1. Process Overview ───────────────────────────────────────────────────

  private static final String PROCESS_OVERVIEW = """
      Supplier Onboarding Process Overview:
      Step 1: A user starts the process by requesting to add a new supplier. A questionnaire pops up for the user to fill out: supplier name, purpose, products/services needed, expected annual volume, urgency, and needed-by date.
      Step 2: The Supplier Agent checks the supplier database (ERP) to see if this supplier already exists or if a similar supplier for this purpose exists.
      Step 3a: If a similar supplier exists, the user is notified and asked if they can continue with the suggested supplier instead of creating a new one.
      Step 3b: If the user is not satisfied or no similar supplier exists, a new supplier request is opened. The user fills out: name, address, purpose, ISO certifications, banking details, etc.
      Step 4: The agent extracts fields from uploaded documents and the supplier's website. The agent then validates the data against the internal supplier policy.
      Step 5: A supplier risk score is calculated. The agent decides the next step: auto-approve, request approval, or request clarification. If approval or missing data is required, the workflow pauses and assigns human tasks.
      Step 5a: GREEN score (>= 80): Supervisor approval and QM/ISM manager approval are requested, then the supplier is added to the ERP system.
      Step 5b: YELLOW score (45-79): The user must add more information. QM/ISM manager may be involved if the user cannot fill out the information alone. When new info arrives, the agent re-evaluates and continues.
      Step 5c: RED score (< 45): The request is declined. The user, supervisor, and QM/ISM manager are all notified.
      Step 6: The agent triggers ERP posting, completes the process, and produces a structured end-summary including decisions made, policies applied, human interventions, and final result.
      """;

  // ── 2. Required Supplier Fields ───────────────────────────────────────────

  private static final String REQUIRED_FIELDS = """
      Required Supplier Registration Fields:
      - Business Name: Legal registered name of the supplier company (mandatory).
      - Legal Form: Type of legal entity, e.g. GmbH, AG, SE, Ltd, SAS (mandatory).
      - VAT ID: EU VAT identification number, e.g. DE123456789 for Germany. Optional but recommended for all EU suppliers.
      - Commercial Register Number: HRB or equivalent company register reference (optional).
      - Business Purpose: Description of what the supplier does and what products/services they supply.
      - Business Address: Street, city, zip code, country (mandatory). Street2, state are optional.
      - Phone and Email: Supplier contact details (optional but recommended).
      - Website: Supplier company website URL (optional).
      - Primary Contact: First name, last name, job title, email, phone of the main contact person.
      - Banking Details: IBAN is mandatory. BIC/SWIFT and bank name are optional but helpful for international transfers.
      - Certifications: ISO 9001, ISO 14001, ISO 27001, GDPR DPA — depending on supplier type.
      - Documents: Supporting documents such as company registration, VAT certificate, ISO certificates, annual report (for large volumes), insurance certificate.
      """;

  // ── 3. Certification Requirements ─────────────────────────────────────────

  private static final String CERTIFICATION_REQUIREMENTS = """
      Supplier Certification Requirements:
      ISO 9001 — Quality Management System: Required for all suppliers with an expected annual volume above EUR 50,000. The supplier must provide a valid certificate number and expiry date. Upload the certificate document.
      ISO 14001 — Environmental Management System: Required for manufacturing suppliers or suppliers with significant environmental impact. Upload the certificate.
      ISO 27001 — Information Security Management: Required for IT suppliers and any supplier that processes, stores, or transmits company data. Upload the certificate.
      GDPR Data Processing Agreement (DPA): Required if the supplier processes personal data on behalf of the company (e.g. cloud services, HR tools, marketing platforms). Upload the signed DPA document.
      How to fill in certifications: For each applicable certification, check the box, enter the certificate number (e.g. QMS-2024-DE-001), set the expiry date, and upload the certificate file using the upload button next to that certification.
      """;

  // ── 4. Risk Scoring Rules ─────────────────────────────────────────────────

  private static final String RISK_SCORING = """
      Supplier Risk Scoring:
      The risk score has three components, each scored 0-100:
      1. Financial Stability (0-100): Assesses the supplier's financial health and viability.
      2. Policy Compliance (0-100): Measures adherence to internal procurement policies.
      3. Certificate Validity (0-100): Checks that required certifications are present, current, and not expired.
      The aggregate score is the average of all three components: (financialStability + policyCompliance + certValidity) / 3.
      Risk Levels:
      GREEN (aggregate >= 80): Low Risk — Approval route. Supervisor and QM/ISM manager are asked to approve. Once approved, the supplier is posted to the ERP system.
      YELLOW (aggregate 45-79): Medium Risk — Clarification needed. The requester or buyer must provide additional information. The process allows up to 3 clarification retries. If the user cannot answer, the QM/ISM manager may be escalated.
      RED (aggregate < 45): High Risk — Automatic decline. The request is rejected and the user, supervisor, and QM/ISM manager are notified with the reason.
      """;

  // ── 5. Document Requirements ──────────────────────────────────────────────

  private static final String DOCUMENT_REQUIREMENTS = """
      Required Documents for Supplier Onboarding:
      - Company Registration Document: Proof of legal incorporation (e.g. Handelsregisterauszug in Germany). Confirms the supplier is a registered legal entity.
      - VAT Certificate: Official VAT registration document from the tax authority.
      - ISO Certificates: Copies of current ISO 9001, ISO 14001, or ISO 27001 certificates if applicable (see certification requirements).
      - Annual Report or Financial Statements: Required for suppliers with expected annual volume above EUR 100,000 to assess financial stability.
      - Insurance Certificate: General liability insurance proof may be required for on-site or high-value suppliers.
      - GDPR Data Processing Agreement: Signed DPA required if supplier processes personal data (see GDPR DPA certification requirement).
      Documents can be uploaded in the supplier registration form using the upload button. Accepted file types include PDF, TXT, and MD for draft parsing.
      """;

  // ── 6. Approval and Routing ────────────────────────────────────────────────

  private static final String APPROVAL_ROUTING = """
      Supplier Onboarding Approval Process:
      GREEN Route (score >= 80): After validation, a supervisor approval task is created. Once the supervisor approves, a second approval task is assigned to the QM/ISM manager. After both approve, the supplier record is automatically posted to the ERP system and the requester is notified of completion.
      YELLOW Route (score 45-79): A clarification task is assigned to the requester or buyer. They must supply missing or corrected information. After submission, the agent re-validates and recalculates the risk score. If the score improves to GREEN, the approval route proceeds. A maximum of 3 clarification retries is allowed before escalation or decline. The QM/ISM manager can be involved if the user cannot answer alone.
      RED Route (score < 45): The supplier request is automatically declined. A structured decline reason is recorded. The requester, their supervisor, and the QM/ISM manager are all notified with the decline summary.
      All routing decisions, approvals, and human interventions are recorded in a structured audit trail attached to the case.
      """;

  // ── 7. Banking and IBAN ────────────────────────────────────────────────────

  private static final String BANKING_DETAILS = """
      Supplier Banking Details:
      IBAN (International Bank Account Number) is mandatory for all suppliers. It is required to process payments in the ERP system.
      IBAN format: Up to 34 alphanumeric characters. Examples: DE89 3704 0044 0532 0130 00 (Germany), FR76 3000 6000 0112 3456 7890 189 (France), GB29 NWBK 6016 1331 9268 19 (UK).
      BIC/SWIFT code: Optional but recommended for international wire transfers. Format: 8 or 11 characters, e.g. DEUTDEDB or DEUTDEDBFRA.
      Bank Name: Optional, helpful for reference.
      Account Holder Name: Should match the registered business name of the supplier.
      """;

  // ── 8. Urgency Levels ─────────────────────────────────────────────────────

  private static final String URGENCY_LEVELS = """
      Supplier Request Urgency Levels:
      NORMAL: Standard processing timeline. No expedited review needed. Used for planned procurement where the supplier is not yet urgently needed.
      HIGH: Expedited review requested. The procurement team and approvers are informed of the higher priority. Suitable when a supplier is needed within a short timeframe.
      CRITICAL: Immediate attention required. All involved roles (approvers, QM/ISM manager) are notified with high-priority flags. Used when operations depend on this supplier being onboarded urgently (e.g. production blockages, emergency sourcing).
      """;

  // ── 9. VAT ID Format ──────────────────────────────────────────────────────

  private static final String VAT_ID_FORMAT = """
      VAT ID (Value Added Tax Identification Number):
      A VAT ID uniquely identifies a business registered for VAT in the EU. It is optional in the supplier form but strongly recommended for all EU-based suppliers.
      Format varies by country: 2-letter country prefix followed by digits or alphanumeric characters.
      Examples: DE123456789 (Germany, 9 digits), FR12345678901 (France, 11 chars), GB123456789 (UK, 9 digits), IT12345678901 (Italy, 11 digits), NL123456789B01 (Netherlands, includes 'B').
      The VAT ID is used for tax verification during cross-reference checks in the validation step.
      """;

  // ── 10. Duplicate Check ────────────────────────────────────────────────────

  private static final String DUPLICATE_CHECK = """
      Supplier Duplicate Check:
      Before creating a new supplier record, the system automatically checks the supplier database for existing or similar suppliers. The check uses supplier name, country, and business purpose to find matches with a similarity score (0-100).
      If a match is found, the requester is shown the matching supplier(s) and can choose to:
      1. Use an existing supplier instead of creating a new one.
      2. Proceed with the new supplier request if the existing match does not meet their needs.
      This check prevents duplicate supplier entries in the ERP and reduces procurement risk.
      """;

  // ── 11. Clarification Process ─────────────────────────────────────────────

  private static final String CLARIFICATION_PROCESS = """
      Supplier Onboarding Clarification Process:
      A clarification task is created when the agent assigns a YELLOW routing decision (risk score 45-79). The task is displayed as: "Clarification required (cycle N of 3) — <supplier name>".
      The requester sees a list of up to 10 clarification items, each belonging to one of three types:
      - DOCUMENT: A missing or expired document or certification must be uploaded. A pre-targeted upload control is shown for the relevant document type (e.g. CERTIFICATION:ISO_9001, DOCUMENT:COMMERCIAL_REGISTER, DOCUMENT:ANNUAL_REPORT).
      - DUPLICATE: A potential ERP duplicate was found; the requester must enter a written explanation.
      - OTHER: A general or policy finding; the requester must enter a written explanation.
      Clarification items are generated from agent validation findings with severity FAILURE, WARNING, INSUFFICIENT, or CLARIFICATION_NEEDED.
      At the clarification screen the requester has three options:
      1. Submit the clarification responses (addresses the items and triggers re-evaluation).
      2. Escalate to QM/ISM Manager — a separate assistance task is assigned to the QM/ISM Manager who can provide additional notes; those notes are recorded and re-evaluation proceeds.
      3. Withdraw the request — the requester cancels the request; it is immediately declined.
      After each clarification cycle the agent re-evaluates and recalculates the risk score. The maximum number of clarification cycles is 3. If the retry count exceeds this maximum the request is automatically declined.
      """;

  // ── 12. Agent Validation Steps ────────────────────────────────────────────

  private static final String AGENT_VALIDATION_STEPS = """
      Agent Validation Steps:
      The Supplier Agent performs four sequential processing steps during the validation stage:
      Step 1 — Document Extraction: The agent reads uploaded documents and parses structured data (business name, address, certifications, banking details, contact information, etc.).
      Step 2 — Policy Validation: Extracted data is validated against the internal supplier onboarding policy. The agent uses the knowledge base (RAG) to check certification requirements, document thresholds, and procurement rules.
      Step 3 — Cross-Reference Checks: The agent verifies the VAT ID format, checks the supplier against the ERP database for duplicates, and runs sanctions screening.
      Step 4 — Risk Score Calculation: The four component scores (financial stability, policy compliance, certificate validity, sanctions compliance) are averaged to produce the aggregate risk score, and the risk level (GREEN/YELLOW/RED) is determined.
      Each step produces validation findings with one of these severities: PASSED (no issue), WARNING (clarification advisable), FAILURE (serious gap that must be resolved), INSUFFICIENT (incomplete data), CLARIFICATION_NEEDED (explicit clarification required).
      Findings that require a document upload carry a document type key in the format CERTIFICATION:<NAME> (e.g. CERTIFICATION:ISO_9001, CERTIFICATION:ISO_14001, CERTIFICATION:ISO_27001) or DOCUMENT:<NAME> (e.g. DOCUMENT:ANNUAL_REPORT, DOCUMENT:COMMERCIAL_REGISTER, DOCUMENT:BANK_STATEMENT). Findings without a document requirement (e.g. duplicate checks, data issues) carry no key.
      """;

  // ── 13. Onboarding Status Lifecycle ──────────────────────────────────────

  private static final String ONBOARDING_STATUS_LIFECYCLE = """
      Supplier Onboarding Status Lifecycle:
      The onboarding request moves through the following status stages:
      REQUEST — The initial questionnaire is in progress. The requester fills in supplier name, business purpose, products/services, expected volume, urgency, and needed-by date.
      DB_CHECK — A duplicate check is being performed against the ERP supplier database.
      SUPPLIER_DATA — The full supplier registration form is being filled in (or the user chose to continue with an existing suggested supplier).
      VALIDATION — The agent is actively performing document extraction and policy validation.
      RISK_SCORING — The agent is calculating the aggregate risk score from the four components.
      APPROVAL_PENDING — The GREEN routing path was selected; the request is waiting for Supervisor approval followed by QM/ISM Manager approval.
      CLARIFICATION_REQUIRED — The YELLOW routing path was selected; the requester must provide additional information before re-evaluation.
      DECLINED — The request was rejected (automatic decline due to RED score, retry limit exceeded, or requester withdrawal).
      COMPLETED — The supplier was successfully validated, approved, and posted to the ERP system.
      """;

  // ── 14. Decline Conditions and Notifications ──────────────────────────────

  private static final String DECLINE_AND_WITHDRAWAL = """
      Supplier Onboarding Decline Conditions and Notifications:
      A supplier onboarding request can be declined in three ways:
      1. Automatic decline (RED score): The agent assigns a RED routing decision when the aggregate risk score is below 45. The Supplier Agent records the decline with reasons taken from validation findings with FAILURE severity (up to 5 reasons). If no FAILURE findings exist, the reason defaults to "Risk score below minimum threshold".
      2. Retry limit exceeded: If the clarification retry count exceeds the maximum of 3 cycles, the request is automatically declined regardless of the current score.
      3. Requester withdrawal: The requester can withdraw the request at any point during a clarification cycle. The decline is recorded as withdrawn by the requester.
      When a request is declined the system:
      - Appends a final AuditTrailEntry to OnboardingRequest.auditTrail containing: decline timestamp, declined-by actor, and the list of decline reasons (taken from FAILURE-severity validation findings, up to 5 reasons).
      - Sends email notifications to: the requester, the supervisor, and the QM Manager.
      - Creates a "Decline review" task so the involved parties can review the decline summary.
      After the decline review task is completed the process routes to the completion screen which shows the full decline summary including the audit trail and notifications sent.
      """;

  // ── Registry ──────────────────────────────────────────────────────────────

  public static List<String> getAll() {
    return List.of(
        PROCESS_OVERVIEW,
        REQUIRED_FIELDS,
        CERTIFICATION_REQUIREMENTS,
        RISK_SCORING,
        DOCUMENT_REQUIREMENTS,
        APPROVAL_ROUTING,
        BANKING_DETAILS,
        URGENCY_LEVELS,
        VAT_ID_FORMAT,
        DUPLICATE_CHECK,
        CLARIFICATION_PROCESS,
        AGENT_VALIDATION_STEPS,
        ONBOARDING_STATUS_LIFECYCLE,
        DECLINE_AND_WITHDRAWAL
    );
  }
}
