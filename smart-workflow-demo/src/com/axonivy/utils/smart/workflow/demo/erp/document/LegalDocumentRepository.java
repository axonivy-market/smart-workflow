package com.axonivy.utils.smart.workflow.demo.erp.document;

import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.smart.workflow.utils.IdGenerationUtils;

import ch.ivyteam.ivy.environment.Ivy;

public class LegalDocumentRepository {

  private static final String FIELD_DOCUMENT_ID = "documentId";
  private static final String FIELD_OBJECT_ID = "objectId";
  private static final String FIELD_DOCUMENT_TYPE = "documentType";

  private static LegalDocumentRepository instance;

  public static LegalDocumentRepository getInstance() {
    if (instance == null) {
      instance = new LegalDocumentRepository();
    }
    return instance;
  }

  public LegalDocument save(LegalDocument document) {
    if (document == null) {
      throw new IllegalArgumentException("LegalDocument cannot be null");
    }
    if (StringUtils.isBlank(document.getDocumentId())) {
      document.setDocumentId(IdGenerationUtils.generateRandomId());
    }
    Ivy.repo().save(document);
    return document;
  }

  public Optional<LegalDocument> findById(String documentId) {
    return Optional.ofNullable(
        Ivy.repo().search(LegalDocument.class)
            .textField(FIELD_DOCUMENT_ID).isEqualToIgnoringCase(documentId)
            .execute().getFirst());
  }

  public List<LegalDocument> findByObjectId(String objectId) {
    return Ivy.repo().search(LegalDocument.class)
        .textField(FIELD_OBJECT_ID).isEqualToIgnoringCase(objectId)
        .execute().getAll();
  }

  public List<LegalDocument> findByObjectIdAndDocumentType(String objectId, LegalDocumentType documentType) {
    return Ivy.repo().search(LegalDocument.class)
        .textField(FIELD_OBJECT_ID).isEqualToIgnoringCase(objectId)
        .and().textField(FIELD_DOCUMENT_TYPE).isEqualToIgnoringCase(documentType.name())
        .execute().getAll();
  }

  public void delete(LegalDocument document) {
    if (document == null) {
      return;
    }
    findById(document.getDocumentId()).ifPresent(Ivy.repo()::delete);
  }
}
