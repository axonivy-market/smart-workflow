package com.axonivy.utils.smart.workflow.demo.supplier.onboarding.bean.interfaces;

import java.util.Map;

import com.axonivy.utils.smart.workflow.demo.document.enums.LegalDocumentType;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.helper.DocumentDisplayHelper;

public interface DocumentDisplaySupport {

  default String getDocumentTypeLabel(String typeKey) {
    return DocumentDisplayHelper.getDocumentTypeLabel(typeKey);
  }

  default String getDocumentTypeSubtitle(String typeKey) {
    return DocumentDisplayHelper.getDocumentTypeSubtitle(typeKey);
  }

  default LegalDocumentType[] getCertificationTypes() {
    return DocumentDisplayHelper.getCertificationTypes();
  }

  default boolean isLegalDocumentTypeRequired(String typeKey) {
    return DocumentDisplayHelper.isLegalDocumentTypeRequired(typeKey);
  }

  default Map<String, Boolean> getLegalDocumentTypeRequired() {
    return DocumentDisplayHelper.getLegalDocumentTypeRequired();
  }

  default String getScoreWidthClass(int score) {
    return DocumentDisplayHelper.getScoreWidthClass(score);
  }
}
