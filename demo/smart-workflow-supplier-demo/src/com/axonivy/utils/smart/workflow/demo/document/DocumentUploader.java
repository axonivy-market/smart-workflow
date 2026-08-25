package com.axonivy.utils.smart.workflow.demo.document;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.smart.workflow.demo.document.enums.LegalDocumentObjectType;
import com.axonivy.utils.smart.workflow.demo.document.enums.LegalDocumentType;
import com.axonivy.utils.smart.workflow.demo.document.repository.LegalDocumentRepository;

import jakarta.faces.event.ActionEvent;

public interface DocumentUploader {

  List<LegalDocument> getSupplierDocuments();

  void setSupplierDocuments(List<LegalDocument> docs);

  String getPendingDocumentType();

  void setPendingDocumentType(String type);

  String getObjectId();

  LegalDocumentObjectType getObjectType();

  default String ensureObjectId() {
    return getObjectId();
  }

  default void onDocumentSaved(LegalDocument doc) {}

  default void onDocumentDeleted(LegalDocument doc) {}

  default void loadDocuments() {
    String id = getObjectId();
    if (StringUtils.isNotBlank(id)) {
      setSupplierDocuments(LegalDocumentRepository.getInstance().findByObjectId(id));
    }
  }

  default LegalDocument saveDocument(LegalDocument doc) {
    LegalDocumentRepository repo = LegalDocumentRepository.getInstance();
    List<LegalDocument> current = new ArrayList<>(getSupplierDocuments());
    Iterator<LegalDocument> it = current.iterator();
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
    if (incoming.getDocumentType().isCertification()
        && incoming.getDocumentType() != LegalDocumentType.CERTIFICATION) {
      return true;
    }
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
