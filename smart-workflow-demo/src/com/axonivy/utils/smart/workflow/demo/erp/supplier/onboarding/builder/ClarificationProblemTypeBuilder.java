package com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.builder;

import com.axonivy.utils.smart.workflow.demo.erp.supplier.model.RiskKind;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.enums.ClarificationProblemType;

public class ClarificationProblemTypeBuilder {

  private ClarificationProblemTypeBuilder() {
  }

  public static ClarificationProblemType resolve(
      String documentTypeKey,
      RiskKind riskKind,
      String source,
      String message) {

    if (isDocumentProblem(documentTypeKey, riskKind)) {
      return ClarificationProblemType.DOCUMENT;
    }
    if (isDuplicateProblem(source, message)) {
      return ClarificationProblemType.DUPLICATE;
    }
    return ClarificationProblemType.OTHER;
  }

  private static boolean isDocumentProblem(String documentTypeKey, RiskKind riskKind) {
    return hasValue(documentTypeKey)
        || riskKind == RiskKind.MISSING_DOC;
  }

  private static boolean isDuplicateProblem(String source, String message) {
    return containsDuplicateKeyword(source)
        || containsDuplicateKeyword(message);
  }

  private static boolean containsDuplicateKeyword(String text) {
    if (!hasValue(text)) {
      return false;
    }
    String normalized = text.toLowerCase();
    return normalized.contains("duplicate")
        || normalized.contains("erp");
  }

  private static boolean hasValue(String value) {
    return value != null && !value.isBlank();
  }
}
