package com.axonivy.utils.smart.workflow.demo.erp.supplier.bean;

import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

import com.axonivy.utils.smart.workflow.demo.erp.document.CertificationUploader;
import com.axonivy.utils.smart.workflow.demo.erp.document.LegalDocument;
import com.axonivy.utils.smart.workflow.demo.erp.document.LegalDocumentType;
import com.axonivy.utils.smart.workflow.demo.erp.document.RequiredDocumentUploader;
import com.axonivy.utils.smart.workflow.demo.erp.shared.Address;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.agent.ValidationRunner;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.model.Supplier;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.model.SupplierBanking;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.model.SupplierContact;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.model.SupplierPolicyRule;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.Country;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.OnboardingRequest;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.ValidationFinding;

/**
 * Abstract base bean providing read-only access to supplier details.
 *
 * <p>Holds all state required by {@code SupplierDetails.xhtml} and
 * {@code LegalDocuments.xhtml}: the onboarding request, reference data
 * (countries, legal forms), and the supplier document list.  Subclasses add
 * entity-context ({@link #getObjectId()}, {@link #getObjectType()}) and any
 * edit or workflow capabilities.
 *
 * @see SupplierRegistrationFormBean
 */
public abstract class ReadOnlySupplierDetailsBean
    implements Serializable, CertificationUploader, RequiredDocumentUploader {

  private static final long serialVersionUID = 1L;

  protected OnboardingRequest request;

  private List<Country> countries;
  private List<String> legalForms;
  private List<LegalDocument> supplierDocuments = new ArrayList<>();
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

  // ── DocumentUploader — state (concrete storage) ───────────────────────────

  @Override
  public List<LegalDocument> getSupplierDocuments() {
    return supplierDocuments;
  }

  @Override
  public void setSupplierDocuments(List<LegalDocument> docs) {
    this.supplierDocuments = docs;
  }

  @Override
  public String getPendingDocumentType() {
    return pendingDocumentType;
  }

  @Override
  public void setPendingDocumentType(String pendingDocumentType) {
    this.pendingDocumentType = pendingDocumentType;
  }

  // ── Policy rules (used by SupplierAgentProcessingDetails dialog) ───────────

  public List<SupplierPolicyRule> getPolicyRules() {
    return ValidationRunner.loadPolicyRules();
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
    LegalDocument doc = supplierDocuments.stream()
        .filter(d -> documentId.equals(d.getDocumentId()))
        .findFirst().orElse(null);
    if (doc == null || doc.getFileContent() == null) {
      return null;
    }
    byte[] content = doc.getFileContent();
    String fileName = doc.getFileName();
    String contentType = doc.getContentType() != null ? doc.getContentType() : "application/octet-stream";
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