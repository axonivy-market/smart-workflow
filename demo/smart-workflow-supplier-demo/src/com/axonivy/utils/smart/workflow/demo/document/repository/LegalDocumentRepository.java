package com.axonivy.utils.smart.workflow.demo.document.repository;

import java.io.ByteArrayInputStream;

import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.smart.workflow.demo.document.LegalDocument;

import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.workflow.document.IDocument;

public class LegalDocumentRepository {

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
    IDocument ivyDoc = Ivy.wfCase().documents().add(document.getFileName());
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
}
