package com.axonivy.utils.smart.workflow.demo.document.repository;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.smart.workflow.demo.document.LegalDocument;
import com.axonivy.utils.smart.workflow.demo.document.enums.LegalDocumentObjectType;
import com.axonivy.utils.smart.workflow.demo.document.enums.LegalDocumentType;

import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.workflow.document.IDocument;

public class LegalDocumentRepository {

  private static final String SEP = ";";
  private static final LegalDocumentRepository instance = new LegalDocumentRepository();

  public static LegalDocumentRepository getInstance() {
    return instance;
  }

  public LegalDocument save(LegalDocument document) {
    if (document == null) {
      throw new IllegalArgumentException("LegalDocument cannot be null");
    }
    IDocument ivyDoc = Ivy.wfCase().documents().add(encodeName(document));
    byte[] content = document.getFileContent();
    if (content != null) {
      ivyDoc.write().withContentFrom(new ByteArrayInputStream(content));
    }
    document.setDocumentId(ivyDoc.uuid());
    document.setFileContent(null);
    return document;
  }

  public void delete(LegalDocument document) {
    if (document == null || StringUtils.isBlank(document.getDocumentId())) {
      return;
    }
    Ivy.wfCase().documents().delete(document.getDocumentId());
  }

  public List<LegalDocument> findByObjectId(String objectId) {
    return findByObjectId(objectId, (String) null);
  }

  public List<LegalDocument> findByObjectId(String objectId, String caseUuid) {
    List<LegalDocument> result = new ArrayList<>();
    try {
      var wfCase = caseUuid != null ? Ivy.wf().findCase(caseUuid) : Ivy.wfCase();
      if (wfCase == null) {
        return result;
      }
      for (IDocument ivyDoc : wfCase.documents().getAll()) {
        LegalDocument ld = decodeName(ivyDoc);
        if (objectId != null && objectId.equals(ld.getObjectId())) {
          result.add(ld);
        }
      }
    } catch (Exception e) {
      Ivy.log().warn("Could not load documents for objectId '" + objectId + "': " + e.getMessage());
    }
    return result;
  }

  public Optional<LegalDocument> findById(String documentId) {
    if (StringUtils.isBlank(documentId)) {
      return Optional.empty();
    }
    IDocument ivyDoc = Ivy.wfCase().documents().get(documentId);
    return ivyDoc != null ? Optional.of(decodeName(ivyDoc)) : Optional.empty();
  }

  private static String encodeName(LegalDocument doc) {
    return String.join(SEP,
        nvl(doc.getFileName()),
        nvl(doc.getObjectId()),
        doc.getObjectType() != null ? doc.getObjectType().name() : "",
        doc.getDocumentType() != null ? doc.getDocumentType().name() : "",
        nvl(doc.getDescription())
    );
  }

  private static LegalDocument decodeName(IDocument ivyDoc) {
    LegalDocument ld = new LegalDocument();
    ld.setDocumentId(ivyDoc.uuid());
    String name = ivyDoc.getName();
    if (name == null) {
      return ld;
    }
    String[] parts = name.split(SEP, -1);
    ld.setFileName(parts.length > 0 ? parts[0] : null);
    ld.setObjectId(parts.length > 1 ? parts[1] : null);
    if (parts.length > 2 && !parts[2].isEmpty()) {
      try {
        ld.setObjectType(LegalDocumentObjectType.valueOf(parts[2]));
      } catch (IllegalArgumentException ignored) {}
    }
    if (parts.length > 3 && !parts[3].isEmpty()) {
      try {
        ld.setDocumentType(LegalDocumentType.valueOf(parts[3]));
      } catch (IllegalArgumentException ignored) {}
    }
    ld.setDescription(parts.length > 4 ? parts[4] : null);
    return ld;
  }

  private static String nvl(String s) {
    return s != null ? s : "";
  }
}
