package com.axonivy.utils.smart.workflow.demo.supplier.onboarding.helper;

import java.util.List;

import com.axonivy.utils.smart.workflow.demo.assistant.AgentGuidance;

public final class SupplierOnboardingGuidance {

  private SupplierOnboardingGuidance() {}

  public static List<AgentGuidance> forRequest() {
    return List.of(
        of("What info do I need for a new supplier?",
            "explain the required fields: supplier business name, legal form, VAT ID, business address, primary contact, and the department/business purpose"),
        of("How does the DB check work?",
            "explain that after submitting the request the system searches the supplier database for similar entries by name and country using the findSimilarSuppliers tool, then presents any matches so the user can decide whether to reuse an existing supplier or proceed with a new registration"),
        of("What is a valid business purpose?",
            "ask the user to describe the procurement purpose, then suggest the most relevant department from the available list and explain what constitutes a clear business purpose (e.g. product category, service type, cost centre)"),
        of("Can you parse my supplier document?",
            "ask the user to upload a .txt or .md file using the upload button, then confirm parsing to auto-fill the form fields"));
  }

  public static List<AgentGuidance> forRegistration() {
    return List.of(
        of("What certifications are required?",
            "use the openSearchSearch tool with collection 'supplier-onboarding-demo'"
                + " to look up certification requirements, then summarize which certifications "
                + "(ISO 9001, ISO 14001, ISO 27001, GDPR DPA) are required and how to fill them in"),
        of("How does risk scoring work?",
            "use the openSearchSearch tool with collection 'supplier-onboarding-demo'"
                + " to look up risk scoring rules, then explain the four components and "
                + "GREEN/YELLOW/RED thresholds"),
        of("What documents do I need to upload?",
            "use the openSearchSearch tool with collection 'supplier-onboarding-demo'"
                + " to look up document requirements, then list what needs to be uploaded"),
        of("Is IBAN required?",
            "use the openSearchSearch tool with collection 'supplier-onboarding-demo'"
                + " to look up banking requirements, then confirm that IBAN is mandatory "
                + "and explain the format"),
        of("What is a VAT ID?",
            "explain what a VAT ID is, give country-specific format examples (e.g. DE123456789 for Germany), "
                + "and clarify that it is optional but recommended for EU suppliers"),
        of("Can you parse my supplier document?",
            "ask the user to upload one or more .txt or .md files using the upload button, "
                + "then confirm parsing to auto-fill the registration form fields"));
  }

  public static List<AgentGuidance> forAgentProcessing() {
    return List.of(
        of("What does my risk score mean?",
            "explain the aggregate risk score shown on this page — what the number means, "
                + "how it is calculated from Financial Stability, Policy Compliance, "
                + "Certificate Validity scores, and what the GREEN/YELLOW/RED thresholds are"),
        of("Why did a validation step fail?",
            "look at the failed or warning validation findings on this page and explain "
                + "what each issue means, why it matters for supplier onboarding, "
                + "and what corrective action the supplier should take"),
        of("What are the next steps?",
            "based on the routing decision shown (APPROVAL, CLARIFICATION, or DECLINE), "
                + "explain what happens next in the onboarding workflow — "
                + "who reviews it, what the supplier needs to provide, and expected timelines"),
        of("What was checked in each step?",
            "describe what each of the four validation steps checks: "
                + "Document Extraction (parsing uploaded documents), "
                + "Policy Validation (checking onboarding rules and required fields), "
                + "Financial Validation (checking financial health indicators against financial rules), "
                + "and Risk Score Calculation (computing the aggregate risk score)"));
  }

  public static List<AgentGuidance> forDuplicateCheck() {
    return List.of(
        of("What does a potential match mean?",
            "explain that the AI found existing suppliers with a similar name, country, or business purpose — the user should review each match and decide if it is the same legal entity or a different supplier"),
        of("How is the match score determined?",
            "explain that the AI compares name similarity, country, VAT ID, and business purpose overlap — a same-country supplier in the same business category is a stronger duplicate signal even if the name differs slightly"),
        of("Should I proceed if a duplicate is found?",
            "explain that if the match is clearly the same legal entity the user should stop and reuse the existing supplier; if it is genuinely a different company despite the similarity, they can confirm and proceed with a new registration"),
        of("What information was compared?",
            "explain that the system searched by business name and country, then the AI analysed name similarity, VAT ID, and business purpose to assess whether it is likely the same supplier"));
  }

  private static AgentGuidance of(String questionPattern, String instruction) {
    AgentGuidance g = new AgentGuidance();
    g.setQuestionPattern(questionPattern);
    g.setInstruction(instruction);
    return g;
  }
}
