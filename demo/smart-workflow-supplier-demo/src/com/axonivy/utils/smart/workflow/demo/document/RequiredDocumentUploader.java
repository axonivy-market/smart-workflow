package com.axonivy.utils.smart.workflow.demo.document;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.primefaces.event.FileUploadEvent;

import com.axonivy.utils.smart.workflow.demo.document.enums.LegalDocumentType;

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
}
