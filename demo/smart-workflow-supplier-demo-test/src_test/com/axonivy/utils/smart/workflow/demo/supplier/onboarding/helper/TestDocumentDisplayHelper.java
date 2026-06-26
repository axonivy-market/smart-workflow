package com.axonivy.utils.smart.workflow.demo.supplier.onboarding.helper;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import org.junit.jupiter.api.Test;

import com.axonivy.utils.smart.workflow.demo.document.enums.LegalDocumentType;

import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
public class TestDocumentDisplayHelper {
 @Test
  void isLegalDocumentTypeRequired_whenTypeKeyIsValid_returnExpectedValue() {
    LegalDocumentType type = LegalDocumentType.values()[0];

    assertEquals(type.isRequired(),
        DocumentDisplayHelper.isLegalDocumentTypeRequired(type.name()));
  }

  @Test
  void isLegalDocumentTypeRequired_whenTypeKeyIsInvalid_returnFalse() {
    assertFalse(DocumentDisplayHelper.isLegalDocumentTypeRequired(null));
    assertFalse(DocumentDisplayHelper.isLegalDocumentTypeRequired("UNKNOWN"));
  }

  @Test
  void getLegalDocumentTypeRequired_whenCalled_returnAllDocumentTypes() {
    Map<String, Boolean> required = DocumentDisplayHelper.getLegalDocumentTypeRequired();

    assertEquals(LegalDocumentType.values().length, required.size());

    for (LegalDocumentType type : LegalDocumentType.values()) {
      assertEquals(type.isRequired(), required.get(type.name()));
    }
  }

  @Test
  void getDocumentTypeLabel_whenSpecialCasesProvided_returnExpectedLabels() {
    assertEquals("Document",
        DocumentDisplayHelper.getDocumentTypeLabel(null));

    assertEquals("Business License",
        DocumentDisplayHelper.getDocumentTypeLabel("DOCUMENT:BUSINESS_LICENSE"));

    assertEquals("ISO 9001 — Quality Management",
        DocumentDisplayHelper.getDocumentTypeLabel("CERTIFICATION:ISO_9001"));

    assertEquals("Company Registration Extract",
        DocumentDisplayHelper.getDocumentTypeLabel(
            LegalDocumentType.COMMERCIAL_REGISTER.name()));

    assertEquals("Custom Document",
        DocumentDisplayHelper.getDocumentTypeLabel("CUSTOM_DOCUMENT"));
  }

  @Test
  void getDocumentTypeSubtitle_whenSpecialCasesProvided_returnExpectedSubtitles() {
    assertEquals("",
        DocumentDisplayHelper.getDocumentTypeSubtitle(null));

    assertEquals("Upload the required Business License document",
        DocumentDisplayHelper.getDocumentTypeSubtitle("DOCUMENT:BUSINESS_LICENSE"));

    assertEquals("Required for suppliers > €50k annual volume",
        DocumentDisplayHelper.getDocumentTypeSubtitle("CERTIFICATION:ISO_9001"));

    assertEquals("Official commercial register document",
        DocumentDisplayHelper.getDocumentTypeSubtitle(
            LegalDocumentType.COMMERCIAL_REGISTER.name()));

    assertEquals("",
        DocumentDisplayHelper.getDocumentTypeSubtitle("CUSTOM_DOCUMENT"));
  }

  @Test
  void getScoreWidthClass_whenScoreRequiresRounding_returnExpectedClass() {
    assertEquals("so-w-0", DocumentDisplayHelper.getScoreWidthClass(-10));
    assertEquals("so-w-50", DocumentDisplayHelper.getScoreWidthClass(48));
    assertEquals("so-w-100", DocumentDisplayHelper.getScoreWidthClass(110));
  }

  @Test
  void formatKeyName_whenNameContainsWords_returnFormattedName() {
    assertEquals("Annual Report",
        DocumentDisplayHelper.formatKeyName("ANNUAL_REPORT"));

    assertEquals("Annual Report",
        DocumentDisplayHelper.formatKeyName("ANNUAL   REPORT"));

    assertEquals("Iso 9001",
        DocumentDisplayHelper.formatKeyName("ISO_9001"));
  }

  @Test
  void formatKeyName_whenNameIsBlank_returnDefaultDocumentLabel() {
    assertEquals("Document",
        DocumentDisplayHelper.formatKeyName(null));

    assertEquals("Document",
        DocumentDisplayHelper.formatKeyName(" "));
  }
}
