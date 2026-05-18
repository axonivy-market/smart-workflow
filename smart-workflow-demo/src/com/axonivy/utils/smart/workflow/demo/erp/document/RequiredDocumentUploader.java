package com.axonivy.utils.smart.workflow.demo.erp.document;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.primefaces.event.FileUploadEvent;

/**
 * Contract for uploading and querying required business documents by type.
 *
 * <p>{@link #uploadRequiredDocument(FileUploadEvent)} is provided as a default:
 * <ul>
 *   <li>When a UI button pre-sets {@link #getPendingDocumentType()}, the format
 *   {@code "TYPE"} or {@code "TYPE:DESCRIPTION"} is parsed (certification uploads
 *   use the latter, e.g. {@code "CERTIFICATION:ISO_9001"}).</li>
 *   <li>When {@code pendingDocumentType} is blank (drag-and-drop without a prior
 *   button click), the document type is inferred from the filename via
 *   {@link LegalDocumentType#fromFileName(String)}.</li>
 * </ul>
 *
 * <p>{@link #getDocumentByType(LegalDocumentType)} and
 * {@link #getAdditionalDocuments()} filter {@link #getSupplierDocuments()}
 * without requiring interface changes when new document slots are added.
 */
public interface RequiredDocumentUploader extends DocumentUploader {

  /** Document types that have dedicated named rows or panels in the UI. */
  Set<LegalDocumentType> MANAGED_TYPES = Set.of(
      LegalDocumentType.COMMERCIAL_REGISTER,
      LegalDocumentType.SELF_DECLARATION,
      LegalDocumentType.ANNUAL_REPORT,
      LegalDocumentType.CERTIFICATION
  );

  default void uploadRequiredDocument(FileUploadEvent event) {
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
        // Resolve cert sub-type key: "CERTIFICATION:ISO_9001" → docType=ISO_9001, description=null
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
      // Drag-and-drop without an explicit type: infer from filename
      docType = LegalDocumentType.fromFileName(event.getFile().getFileName());
    }

    LegalDocument doc = LegalDocument.builder()
        .objectId(ensureObjectId())
        .objectType(getObjectType())
        .documentType(docType)
        .fileName(event.getFile().getFileName())
        .contentType(event.getFile().getContentType())
        .fileContent(event.getFile().getContent())
        .fileSize(event.getFile().getSize())
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

  /** Returns documents that are not rendered in any named fixed row or the certifications panel. */
  default List<LegalDocument> getAdditionalDocuments() {
    return getSupplierDocuments().stream()
        .filter(d -> !MANAGED_TYPES.contains(d.getDocumentType()) && !d.getDocumentType().isCertification())
        .collect(Collectors.toList());
  }
}
