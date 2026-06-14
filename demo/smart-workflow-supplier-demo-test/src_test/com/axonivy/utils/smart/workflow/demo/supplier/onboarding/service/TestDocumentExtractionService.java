package com.axonivy.utils.smart.workflow.demo.supplier.onboarding.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.axonivy.utils.smart.workflow.demo.document.LegalDocument;
import com.axonivy.utils.smart.workflow.demo.document.enums.LegalDocumentType;

import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
class TestDocumentExtractionService {

  @Test
  void loadDocuments_returnsProvidedList() {
    List<LegalDocument> docs = List.of(new LegalDocument());
    assertThat(DocumentExtractionService.loadDocuments(docs)).isSameAs(docs);
  }

  @Test
  void loadRequiredDocumentTypes_includesRequiredExcludesOthers() {
    List<String> required = DocumentExtractionService.loadRequiredDocumentTypes();

    assertThat(required).contains(
        LegalDocumentType.COMMERCIAL_REGISTER.getLabel(),
        LegalDocumentType.SELF_DECLARATION.getLabel(),
        LegalDocumentType.ANNUAL_REPORT.getLabel(),
        LegalDocumentType.ISO_9001.getLabel());
    assertThat(required).doesNotContain(
        LegalDocumentType.OTHER.getLabel(),
        LegalDocumentType.CONTRACT.getLabel());
  }

  @Test
  void buildDocumentContext_withMultipleDocs_includesAllFileNames() {
    LegalDocument doc1 = new LegalDocument();
    doc1.setFileName("first.pdf");
    doc1.setDocumentType(LegalDocumentType.CONTRACT);

    LegalDocument doc2 = new LegalDocument();
    doc2.setFileName("second.pdf");
    doc2.setDocumentType(LegalDocumentType.ANNUAL_REPORT);

    assertThat(DocumentExtractionService.buildDocumentContext(List.of(doc1, doc2)))
        .contains("first.pdf", "second.pdf");
  }

  @Test
  void buildDocumentContext_withTextContent_includesContentInOutput() {
    LegalDocument doc = new LegalDocument();
    doc.setFileName("report.pdf");
    doc.setDocumentType(LegalDocumentType.ANNUAL_REPORT);
    doc.setFileContent("Revenue: 5M EUR".getBytes(StandardCharsets.UTF_8));

    assertThat(DocumentExtractionService.buildDocumentContext(List.of(doc)))
        .contains("Revenue: 5M EUR");
  }
}
