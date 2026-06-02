package com.axonivy.utils.smart.workflow.demo.erp.document;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.faces.event.ActionEvent;

import org.apache.commons.lang3.StringUtils;

/**
 * Base contract for managing legal document state in a form bean.
 *
 * <p>Implementors must provide:
 * <ul>
 *   <li>{@link #getSupplierDocuments()} / {@link #setSupplierDocuments} — in-memory list</li>
 *   <li>{@link #getPendingDocumentType()} / {@link #setPendingDocumentType} — upload slot</li>
 *   <li>{@link #getObjectId()} — entity ID (e.g. supplierId)</li>
 *   <li>{@link #getObjectType()} — entity type discriminator</li>
 * </ul>
 *
 * <p>Override {@link #ensureObjectId()} to auto-generate an ID on first upload.
 * Override {@link #onDocumentSaved} / {@link #onDocumentDeleted} to keep the
 * entity's document-ID list in sync.
 *
 * @see RequiredDocumentUploader
 * @see CertificationUploader
 */
public interface DocumentUploader {

  // ── Required state (implementor stores in fields) ─────────────────────────

  List<LegalDocument> getSupplierDocuments();

  void setSupplierDocuments(List<LegalDocument> docs);

  String getPendingDocumentType();

  void setPendingDocumentType(String type);

  // ── Entity context (implementor provides) ─────────────────────────────────

  String getObjectId();

  LegalDocumentObjectType getObjectType();

  /**
   * Returns a guaranteed non-blank object ID.
   * The default simply returns {@link #getObjectId()}.
   * Override to auto-generate an ID when the entity has not been persisted yet.
   */
  default String ensureObjectId() {
    return getObjectId();
  }

  // ── Hooks ─────────────────────────────────────────────────────────────────

  /** Called after a document is saved. Override to sync the entity's ID list. */
  default void onDocumentSaved(LegalDocument doc) {}

  /** Called after a document is deleted. Override to sync the entity's ID list. */
  default void onDocumentDeleted(LegalDocument doc) {}

  // ── Default document management ───────────────────────────────────────────

  default void loadDocuments() {
    String id = getObjectId();
    if (StringUtils.isNotBlank(id)) {
      setSupplierDocuments(LegalDocumentRepository.getInstance().findByObjectId(id));
    }
  }

  default LegalDocument saveDocument(LegalDocument doc) {
    LegalDocumentRepository repo = LegalDocumentRepository.getInstance();
    List<LegalDocument> current = new ArrayList<>(getSupplierDocuments());
    // Replace any existing document occupying the same UI slot before adding the new one
    java.util.Iterator<LegalDocument> it = current.iterator();
    while (it.hasNext()) {
      LegalDocument existing = it.next();
      if (isSameSlot(existing, doc)) {
        repo.delete(existing);
        it.remove();
        onDocumentDeleted(existing);
      }
    }
    repo.save(doc);
    current.add(doc);
    setSupplierDocuments(current);
    onDocumentSaved(doc);
    return doc;
  }

  static boolean isSameSlot(LegalDocument existing, LegalDocument incoming) {
    if (!existing.getDocumentType().equals(incoming.getDocumentType())) {
      return false;
    }
    // Specific cert sub-types: same documentType = same slot (one doc per cert type)
    if (incoming.getDocumentType().isCertification()
        && incoming.getDocumentType() != LegalDocumentType.CERTIFICATION) {
      return true;
    }
    // Generic CERTIFICATION: use description to disambiguate
    if (LegalDocumentType.CERTIFICATION.equals(incoming.getDocumentType())) {
      return java.util.Objects.equals(existing.getDescription(), incoming.getDescription());
    }
    return LegalDocumentType.COMMERCIAL_REGISTER.equals(incoming.getDocumentType())
        || LegalDocumentType.SELF_DECLARATION.equals(incoming.getDocumentType())
        || LegalDocumentType.ANNUAL_REPORT.equals(incoming.getDocumentType());
  }

  default void removeSupplierDocument(ActionEvent event) {
    String documentId = (String) event.getComponent().getAttributes().get("documentId");
    LegalDocumentRepository repo = LegalDocumentRepository.getInstance();
    repo.findById(documentId).ifPresent(doc -> {
      repo.delete(doc);
      setSupplierDocuments(getSupplierDocuments().stream()
          .filter(d -> !documentId.equals(d.getDocumentId()))
          .collect(Collectors.toList()));
      onDocumentDeleted(doc);
    });
  }
}
