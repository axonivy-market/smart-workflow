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

  @ParameterizedTest(name = "{0}")
  @MethodSource("uploadScenarios")
  void uploadRequiredDocument_resolvesTypeAndDescription(String testName, String pendingType, String fileName,
      LegalDocumentType expectedType, String expectedDescription) {
    var uploader = uploaderWithPending(pendingType);
    uploader.uploadRequiredDocument(fileName, new byte[0]);
    LegalDocument doc = uploader.getSupplierDocuments().get(0);
    assertThat(doc.getDocumentType()).as(testName).isEqualTo(expectedType);
    assertThat(doc.getDescription()).as(testName).isEqualTo(expectedDescription);
  }

  @SuppressWarnings("unused")
  static Stream<Arguments> uploadScenarios() {
    return Stream.of(
        Arguments.of("uploadRequiredDocument_whenEmptyPending_resolvesFromFileName",  "", "iso9001_cert.pdf", LegalDocumentType.ISO_9001, null),
        Arguments.of("uploadRequiredDocument_whenCommercialRegister_setsType",        "COMMERCIAL_REGISTER", "file.pdf", LegalDocumentType.COMMERCIAL_REGISTER, null),
        Arguments.of("uploadRequiredDocument_whenCertificationWithEnumValue_setsType","CERTIFICATION:ISO_9001", "file.pdf", LegalDocumentType.ISO_9001, null),
        Arguments.of("uploadRequiredDocument_whenCertificationWithFreeText_setsDesc", "CERTIFICATION:BRC Food", "file.pdf", LegalDocumentType.CERTIFICATION, "BRC Food"),
        Arguments.of("uploadRequiredDocument_whenAnnualReportWithText_setsDesc",      "ANNUAL_REPORT:sometext", "file.pdf", LegalDocumentType.ANNUAL_REPORT, "sometext")
    );
  }

  @Test
  void uploadRequiredDocument_clearsPendingTypeAfterUpload() {
    var uploader = uploaderWithPending("COMMERCIAL_REGISTER");
    uploader.uploadRequiredDocument("file.pdf", new byte[0]);
    assertThat(uploader.getPendingDocumentType()).isNull();
  }

  @Test
  void getAdditionalDocuments_whenAllDocumentsAreManagedTypes_returnsEmpty() {
    var uploader = uploaderWithDocs(
        docOfType(LegalDocumentType.COMMERCIAL_REGISTER),
        docOfType(LegalDocumentType.ANNUAL_REPORT));

    assertThat(uploader.getAdditionalDocuments()).isEmpty();
  }

  @Test
  void getAdditionalDocuments_whenExtraDocumentExists_returnsIt() {
    LegalDocument contract = docOfType(LegalDocumentType.CONTRACT);
    var uploader = uploaderWithDocs(
        docOfType(LegalDocumentType.COMMERCIAL_REGISTER),
        contract);

    assertThat(uploader.getAdditionalDocuments()).containsExactly(contract);
  }

  private static LegalDocument docOfType(LegalDocumentType type) {
    LegalDocument doc = new LegalDocument();
    doc.setDocumentType(type);
    return doc;
  }

  private static RequiredDocumentUploader uploaderWithDocs(LegalDocument... initialDocs) {
    return new RequiredDocumentUploader() {
      private final List<LegalDocument> docs = new ArrayList<>(List.of(initialDocs));
      private String pending = null;

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
