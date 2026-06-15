package com.axonivy.utils.smart.workflow.demo.document;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.axonivy.utils.smart.workflow.demo.document.enums.LegalDocumentObjectType;
import com.axonivy.utils.smart.workflow.demo.document.enums.LegalDocumentType;

import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
class TestRequiredDocumentUploader {

  @ParameterizedTest
  @MethodSource("uploadScenarios")
  void uploadRequiredDocument_resolvesTypeAndDescription(String pendingType, String fileName,
      LegalDocumentType expectedType, String expectedDescription) {
    var uploader = uploaderWithPending(pendingType);
    uploader.uploadRequiredDocument(fileName, "application/pdf", new byte[0], 0);
    LegalDocument doc = uploader.getSupplierDocuments().get(0);
    assertThat(doc.getDocumentType()).isEqualTo(expectedType);
    assertThat(doc.getDescription()).isEqualTo(expectedDescription);
  }

  @SuppressWarnings("unused")
  static Stream<Arguments> uploadScenarios() {
    return Stream.of(
        Arguments.of("", "iso9001_cert.pdf", LegalDocumentType.ISO_9001, null),
        Arguments.of("COMMERCIAL_REGISTER", "file.pdf", LegalDocumentType.COMMERCIAL_REGISTER, null),
        Arguments.of("CERTIFICATION:ISO_9001", "file.pdf", LegalDocumentType.ISO_9001, null),
        Arguments.of("CERTIFICATION:BRC Food", "file.pdf", LegalDocumentType.CERTIFICATION, "BRC Food"),
        Arguments.of("ANNUAL_REPORT:sometext", "file.pdf", LegalDocumentType.ANNUAL_REPORT, "sometext")
    );
  }

  @Test
  void uploadRequiredDocument_clearsPendingTypeAfterUpload() {
    var uploader = uploaderWithPending("COMMERCIAL_REGISTER");
    uploader.uploadRequiredDocument("file.pdf", "application/pdf", new byte[0], 0);
    assertThat(uploader.getPendingDocumentType()).isNull();
  }

  private static RequiredDocumentUploader uploaderWithPending(String pendingType) {
    return new RequiredDocumentUploader() {
      private final List<LegalDocument> docs = new ArrayList<>();
      private String pending = pendingType;

      @Override
      public List<LegalDocument> getSupplierDocuments() { return docs; }

      @Override
      public void setSupplierDocuments(List<LegalDocument> d) {
        docs.clear();
        docs.addAll(d);
      }

      @Override
      public String getPendingDocumentType() { return pending; }

      @Override
      public void setPendingDocumentType(String t) { pending = t; }

      @Override
      public String getObjectId() { return "test-obj"; }

      @Override
      public LegalDocumentObjectType getObjectType() { return LegalDocumentObjectType.SUPPLIER; }

      @Override
      public LegalDocument saveDocument(LegalDocument doc) {
        docs.add(doc);
        return doc;
      }
    };
  }
}
