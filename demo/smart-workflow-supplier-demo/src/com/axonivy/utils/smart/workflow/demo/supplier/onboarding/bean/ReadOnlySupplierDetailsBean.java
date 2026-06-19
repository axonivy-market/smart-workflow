package com.axonivy.utils.smart.workflow.demo.supplier.onboarding.bean;

import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

import com.axonivy.utils.smart.workflow.demo.common.Address;
import com.axonivy.utils.smart.workflow.demo.document.CertificationUploader;
import com.axonivy.utils.smart.workflow.demo.document.LegalDocument;
import com.axonivy.utils.smart.workflow.demo.document.RequiredDocumentUploader;
import com.axonivy.utils.smart.workflow.demo.document.enums.LegalDocumentType;
import com.axonivy.utils.smart.workflow.demo.supplier.Supplier;
import com.axonivy.utils.smart.workflow.demo.supplier.SupplierBanking;
import com.axonivy.utils.smart.workflow.demo.supplier.SupplierContact;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.OnboardingRequest;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.ValidationFinding;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.enums.Country;

/**
 * Abstract base bean providing read-only access to supplier details.
 *
 * <p>Holds all state required by {@code SupplierDetails.xhtml} and
 * {@code LegalDocuments.xhtml}: the onboarding request, reference data
 * (countries, legal forms), and the supplier document list. Subclasses add
 * entity-context ({@link #getObjectId()}, {@link #getObjectType()}) and any
 * edit or workflow capabilities.
 *
 * @see SupplierRegistrationFormBean
 */
public abstract class ReadOnlySupplierDetailsBean
    implements Serializable, CertificationUploader, RequiredDocumentUploader {

  private static final long serialVersionUID = 1L;

  protected OnboardingRequest request;

  private List<LegalDocument> supplierDocuments = new ArrayList<>();
  private List<Country> countries;
  private List<String> legalForms;
  private String pendingDocumentType;

  // ── Initialisation ────────────────────────────────────────────────────────

  protected void init(OnboardingRequest request) {
    if (this.request != null) {
      return;
    }
    this.request = request;
    ensureNestedObjectsExist();

    countries = Arrays.asList(Country.values());
    legalForms = Arrays.asList(
        "GmbH", "AG", "GmbH & Co. KG", "SE", "UG", "KG", "OHG", "e.K.", "Ltd.", "S.A.", "B.V.", "Other");

    loadDocuments();
  }

  protected void ensureNestedObjectsExist() {
    if (request.getSupplier() == null) {
      request.setSupplier(new Supplier());
    }
    Supplier s = request.getSupplier();
    if (s.getBusinessAddress() == null) {
      s.setBusinessAddress(new Address());
    }
    if (s.getPrimaryContact() == null) {
      s.setPrimaryContact(new SupplierContact());
    }
    if (s.getBanking() == null) {
      s.setBanking(new SupplierBanking());
    }
    if (s.getCertifications() == null) {
      s.setCertifications(new ArrayList<>());
    }
  }

  // ── DocumentUploader — state (delegates to process data) ─────────────────

  @Override
  public List<LegalDocument> getSupplierDocuments() {
    return supplierDocuments;
  }

  @Override
  public void setSupplierDocuments(List<LegalDocument> docs) {
    this.supplierDocuments = docs != null ? docs : new ArrayList<>();
  }

  @Override
  public String getPendingDocumentType() {
    return pendingDocumentType;
  }

  @Override
  public void setPendingDocumentType(String pendingDocumentType) {
    this.pendingDocumentType = pendingDocumentType;
  }

  // ── Data accessors used by SupplierDetails.xhtml ──────────────────────────

  public OnboardingRequest getRequest() {
    return request;
  }

  public List<ValidationFinding> getPolicyValidationFindings() {
    if (request == null || request.getPolicyValidationFindings() == null) {
      return java.util.Collections.emptyList();
    }
    return request.getPolicyValidationFindings();
  }

  public Supplier getSupplier() {
    return request != null ? request.getSupplier() : null;
  }

  public List<Country> getCountries() {
    return countries;
  }

  public List<String> getLegalForms() {
    return legalForms;
  }

  public LegalDocumentType[] getCertificationTypes() {
    return LegalDocumentType.certificationValues();
  }

  public boolean isLegalDocumentTypeRequired(String typeKey) {
    try {
      return LegalDocumentType.valueOf(typeKey).isRequired();
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  public java.util.Map<String, Boolean> getLegalDocumentTypeRequired() {
    java.util.Map<String, Boolean> map = new java.util.HashMap<>();
    for (LegalDocumentType type : LegalDocumentType.values()) {
      map.put(type.name(), type.isRequired());
    }
    return map;
  }

  // ── Required-document convenience accessors (used by LegalDocuments.xhtml) ──

  public LegalDocument getCompanyRegistrationDoc() {
    return getDocumentByType(LegalDocumentType.COMMERCIAL_REGISTER);
  }

  public LegalDocument getSelfDeclarationDoc() {
    return getDocumentByType(LegalDocumentType.SELF_DECLARATION);
  }

  public LegalDocument getAnnualReportDoc() {
    return getDocumentByType(LegalDocumentType.ANNUAL_REPORT);
  }

  // ── Document lookup by raw typeKey ────────────────────────────────────────

  /**
   * Returns the document matching a raw documentTypeKey string.
   * Handles both "CERTIFICATION:ISO_9001" style and plain "COMMERCIAL_REGISTER" etc.
   */
  public LegalDocument getDocumentByTypeKey(String typeKey) {
    if (typeKey == null) return null;
    if (typeKey.startsWith("CERTIFICATION:")) {
      String certName = typeKey.substring("CERTIFICATION:".length());
      try {
        LegalDocumentType certType = LegalDocumentType.valueOf(certName);
        return getDocumentByType(certType);
      } catch (IllegalArgumentException e) {
        return null;
      }
    }
    if (typeKey.startsWith("DOCUMENT:")) {
      String docName = typeKey.substring("DOCUMENT:".length());
      return getSupplierDocuments().stream()
          .filter(d -> docName.equalsIgnoreCase(d.getDescription()))
          .findFirst().orElse(null);
    }
    try {
      LegalDocumentType type = LegalDocumentType.valueOf(typeKey);
      return getDocumentByType(type);
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  public String getDocumentTypeLabel(String typeKey) {
    if (typeKey == null) return "Document";
    if (typeKey.startsWith("CERTIFICATION:")) {
      String certName = typeKey.substring("CERTIFICATION:".length());
      try { return LegalDocumentType.valueOf(certName).getLabel(); } catch (Exception e) { /* fall through */ }
      return formatKeyName(certName) + " Certificate";
    }
    if (typeKey.startsWith("DOCUMENT:")) {
      return formatKeyName(typeKey.substring("DOCUMENT:".length()));
    }
    return switch (typeKey) {
      case "COMMERCIAL_REGISTER" -> "Company Registration Extract";
      case "SELF_DECLARATION"    -> "Self-Declaration";
      case "ANNUAL_REPORT"       -> "Last Annual Report";
      default                    -> formatKeyName(typeKey);
    };
  }

  public String getDocumentTypeSubtitle(String typeKey) {
    if (typeKey == null) return "";
    if (typeKey.startsWith("CERTIFICATION:")) {
      String certName = typeKey.substring("CERTIFICATION:".length());
      try { return LegalDocumentType.valueOf(certName).getSubtitle(); } catch (Exception e) { /* fall through */ }
      return "Upload a valid " + formatKeyName(certName) + " certificate";
    }
    if (typeKey.startsWith("DOCUMENT:")) {
      return "Upload the required " + formatKeyName(typeKey.substring("DOCUMENT:".length())) + " document";
    }
    return switch (typeKey) {
      case "COMMERCIAL_REGISTER" -> "Official commercial register document";
      case "SELF_DECLARATION"    -> "Confirm compliance with procurement policy";
      case "ANNUAL_REPORT"       -> "Most recent fiscal year financial report";
      default                    -> "";
    };
  }

  private static String formatKeyName(String name) {
    if (name == null || name.isBlank()) return "Document";
    String[] words = name.replace('_', ' ').toLowerCase().split(" ");
    StringBuilder sb = new StringBuilder();
    for (String word : words) {
      if (!word.isEmpty()) {
        if (sb.length() > 0) sb.append(' ');
        sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
      }
    }
    return sb.toString();
  }

  public StreamedContent downloadDocument(String documentId) {
    LegalDocument doc = getSupplierDocuments().stream()
        .filter(d -> documentId.equals(d.getDocumentId()))
        .findFirst().orElse(null);
    if (doc == null || doc.getFileContent() == null) {
      return null;
    }
    byte[] content = doc.getFileContent();
    String fileName = doc.getFileName();
    String contentType = "application/octet-stream";
    return DefaultStreamedContent.builder()
        .name(fileName)
        .contentType(contentType)
        .stream(() -> new ByteArrayInputStream(content))
        .build();
  }

  // ── Score bar width class ──────────────────────────────────────────────

  public String getScoreWidthClass(int score) {
    int rounded = (int) (Math.round(score / 5.0) * 5);
    rounded = Math.max(0, Math.min(100, rounded));
    return "so-w-" + rounded;
  }
}
