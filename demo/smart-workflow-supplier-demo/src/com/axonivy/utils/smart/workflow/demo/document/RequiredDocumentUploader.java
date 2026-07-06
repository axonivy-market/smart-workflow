package com.axonivy.utils.smart.workflow.demo.document;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

import com.axonivy.utils.smart.workflow.demo.document.enums.LegalDocumentType;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.helper.DocumentDisplayHelper;

public interface RequiredDocumentUploader extends DocumentUploader {

  Set<LegalDocumentType> MANAGED_TYPES = Set.of(
      LegalDocumentType.COMMERCIAL_REGISTER,
      LegalDocumentType.SELF_DECLARATION,
      LegalDocumentType.ANNUAL_REPORT,
      LegalDocumentType.CERTIFICATION
  );

  default void uploadRequiredDocument(FileUploadEvent event) {
    uploadRequiredDocument(
        event.getFile().getFileName(),
        event.getFile().getContent());
  }

  default void uploadRequiredDocument(String fileName, byte[] fileContent) {
    LegalDocumentType docType;
    String description = null;

    if (StringUtils.isNotBlank(getPendingDocumentType())) {
      String[] parts = getPendingDocumentType().split(":", 2);
      try {
        docType = LegalDocumentType.valueOf(parts[0]);
      } catch (IllegalArgumentException e) {
        docType = LegalDocumentType.OTHER;
      }
      if (parts.length > 1) {
        try {
          LegalDocumentType subType = LegalDocumentType.valueOf(parts[1]);
          if (subType.isCertification()) {
            docType = subType;
          } else {
            description = parts[1];
          }
        } catch (IllegalArgumentException e) {
          description = parts[1];
        }
      }
    } else {
      docType = LegalDocumentType.fromFileName(fileName);
    }

    LegalDocument doc = LegalDocumentBuilder.builder()
        .objectId(ensureObjectId())
        .objectType(getObjectType())
        .documentType(docType)
        .fileName(fileName)
        .fileContent(fileContent)
        .description(description)
        .uploadedNow()
        .build();
    saveDocument(doc);
    setPendingDocumentType(null);
  }

  default LegalDocument getDocumentByType(LegalDocumentType type) {
    return getSupplierDocuments().stream()
        .filter(d -> type.equals(d.getDocumentType()))
        .findFirst().orElse(null);
  }

  default List<LegalDocument> getAdditionalDocuments() {
    return getSupplierDocuments().stream()
        .filter(d -> !MANAGED_TYPES.contains(d.getDocumentType()) && !d.getDocumentType().isCertification())
        .collect(Collectors.toList());
  }

  default LegalDocument getDocumentByTypeKey(String typeKey) {
    if (typeKey == null) {
      return null;
    }
    if (typeKey.startsWith(DocumentDisplayHelper.CERT_PREFIX)) {
      String certName = typeKey.substring(DocumentDisplayHelper.CERT_PREFIX.length());
      try {
        return getDocumentByType(LegalDocumentType.valueOf(certName));
      } catch (IllegalArgumentException e) {
        return null;
      }
    }
    if (typeKey.startsWith(DocumentDisplayHelper.DOC_PREFIX)) {
      String docName = typeKey.substring(DocumentDisplayHelper.DOC_PREFIX.length());
      return getSupplierDocuments().stream()
          .filter(doc -> docName.equalsIgnoreCase(doc.getDescription()))
          .findFirst().orElse(null);
    }
    try {
      return getDocumentByType(LegalDocumentType.valueOf(typeKey));
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  default LegalDocument getCompanyRegistrationDoc() {
    return getDocumentByType(LegalDocumentType.COMMERCIAL_REGISTER);
  }

  default LegalDocument getSelfDeclarationDoc() {
    return getDocumentByType(LegalDocumentType.SELF_DECLARATION);
  }

  default LegalDocument getAnnualReportDoc() {
    return getDocumentByType(LegalDocumentType.ANNUAL_REPORT);
  }

  default StreamedContent downloadDocument(String documentId) {
    LegalDocument doc = getSupplierDocuments().stream()
        .filter(d -> documentId.equals(d.getDocumentId()))
        .findFirst().orElse(null);
    if (doc == null || doc.getFileContent() == null) {
      return null;
    }
    byte[] content = doc.getFileContent();
    String fileName = doc.getFileName();
    return DefaultStreamedContent.builder()
        .name(fileName)
        .contentType("application/octet-stream")
        .stream(() -> new ByteArrayInputStream(content))
        .build();
  }
}
