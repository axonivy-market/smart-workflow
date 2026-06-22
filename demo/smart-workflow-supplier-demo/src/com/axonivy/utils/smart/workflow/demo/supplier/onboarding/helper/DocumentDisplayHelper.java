package com.axonivy.utils.smart.workflow.demo.supplier.onboarding.helper;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.smart.workflow.demo.document.enums.LegalDocumentType;

import ch.ivyteam.ivy.environment.Ivy;

public final class DocumentDisplayHelper {

  public static final String CERT_PREFIX = "CERTIFICATION:";
  public static final String DOC_PREFIX  = "DOCUMENT:";
  private static final String DEFAULT_DOCUMENT_LABEL = "Document";
  private static final String UNKNOWN_CERTIFICATION_TYPE_LABEL_FORMAT = "Unknown certification type in label lookup: %s";
  private static final String CERTIFICATE_LABEL_FORMAT = "%s Certificate";
  private static final String CERTIFICATE_SUBTITLE_FORMAT = "Upload a valid %s certificate";
  private static final String DOCUMENT_SUBTITLE_FORMAT = "Upload the required %s document";
  private static final String SCORE_WIDTH_CLASS_FORMAT = "so-w-%s";


  private DocumentDisplayHelper() {}

  public static LegalDocumentType[] getCertificationTypes() {
    return LegalDocumentType.certificationValues();
  }

  public static boolean isLegalDocumentTypeRequired(String typeKey) {
    if (typeKey == null) {
      return false;
    }

    try {
      return LegalDocumentType.valueOf(typeKey).isRequired();
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  public static Map<String, Boolean> getLegalDocumentTypeRequired() {
    Map<String, Boolean> map = new HashMap<>();
    for (LegalDocumentType type : LegalDocumentType.values()) {
      map.put(type.name(), type.isRequired());
    }
    return map;
  }

  public static String getDocumentTypeLabel(String typeKey) {
    if (typeKey == null) {
      return DEFAULT_DOCUMENT_LABEL;
    }
    if (typeKey.startsWith(CERT_PREFIX)) {
      String certName = typeKey.substring(CERT_PREFIX.length());
      try {
        return LegalDocumentType.valueOf(certName).getLabel();
      } catch (IllegalArgumentException e) {
        Ivy.log().warn(String.format(UNKNOWN_CERTIFICATION_TYPE_LABEL_FORMAT, certName));
      }
      return String.format(CERTIFICATE_LABEL_FORMAT, formatKeyName(certName));
    }
    if (typeKey.startsWith(DOC_PREFIX)) {
      return formatKeyName(typeKey.substring(DOC_PREFIX.length()));
    }
    try {
      return switch (LegalDocumentType.valueOf(typeKey)) {
        case COMMERCIAL_REGISTER -> "Company Registration Extract";
        case SELF_DECLARATION    -> "Self-Declaration";
        case ANNUAL_REPORT       -> "Last Annual Report";
        default                  -> formatKeyName(typeKey);
      };
    } catch (IllegalArgumentException ignored) {
      return formatKeyName(typeKey);
    }
  }

  public static String getDocumentTypeSubtitle(String typeKey) {
    if (typeKey == null) {
      return "";
    }
    if (typeKey.startsWith(CERT_PREFIX)) {
      String certName = typeKey.substring(CERT_PREFIX.length());
      try {
        return LegalDocumentType.valueOf(certName).getSubtitle();
      } catch (IllegalArgumentException e) {
        Ivy.log().warn(String.format(UNKNOWN_CERTIFICATION_TYPE_LABEL_FORMAT, certName));
      }
      return String.format(CERTIFICATE_SUBTITLE_FORMAT, formatKeyName(certName));
    }
    if (typeKey.startsWith(DOC_PREFIX)) {
      return String.format(DOCUMENT_SUBTITLE_FORMAT, formatKeyName(typeKey.substring(DOC_PREFIX.length())));
    }
    try {
      return switch (LegalDocumentType.valueOf(typeKey)) {
        case COMMERCIAL_REGISTER -> "Official commercial register document";
        case SELF_DECLARATION    -> "Confirm compliance with procurement policy";
        case ANNUAL_REPORT       -> "Most recent fiscal year financial report";
        default                  -> "";
      };
    } catch (IllegalArgumentException ignored) {
      return "";
    }
  }

  public static String getScoreWidthClass(int score) {
    int rounded = (int) (Math.round(score / 5.0) * 5);
    rounded = Math.max(0, Math.min(100, rounded));
    return String.format(SCORE_WIDTH_CLASS_FORMAT, rounded);
  }

  static String formatKeyName(String name) {
  if (name == null || name.isBlank()) {
    return DEFAULT_DOCUMENT_LABEL;
  }

  return Arrays.stream(name.replace('_', ' ').split("\\s+"))
      .map(word -> StringUtils.capitalize(word.toLowerCase()))
      .collect(Collectors.joining(" "));
  }
}
