package com.axonivy.utils.smart.workflow.demo.document;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.axonivy.utils.smart.workflow.demo.document.enums.LegalDocumentObjectType;
import com.axonivy.utils.smart.workflow.demo.document.enums.LegalDocumentType;

import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
class TestLegalDocumentBuilder {

  @Test
  void build_setsAllFields() {
    byte[] content = "contract text".getBytes(StandardCharsets.UTF_8);

    LegalDocument doc = LegalDocumentBuilder.builder()
        .objectId("OBJ-001")
        .objectType(LegalDocumentObjectType.SUPPLIER)
        .documentType(LegalDocumentType.CONTRACT)
        .fileName("contract.pdf")
        .contentType("application/pdf")
        .fileContent(content)
        .fileSize(content.length)
        .description("Main contract")
        .build();

    assertThat(doc.getObjectId()).isEqualTo("OBJ-001");
    assertThat(doc.getObjectType()).isEqualTo(LegalDocumentObjectType.SUPPLIER);
    assertThat(doc.getDocumentType()).isEqualTo(LegalDocumentType.CONTRACT);
    assertThat(doc.getFileName()).isEqualTo("contract.pdf");
    assertThat(doc.getContentType()).isEqualTo("application/pdf");
    assertThat(doc.getFileContent()).isEqualTo(content);
    assertThat(doc.getFileSize()).isEqualTo(content.length);
    assertThat(doc.getDescription()).isEqualTo("Main contract");
  }

  @Test
  void build_minimal_otherFieldsAreNull() {
    LegalDocument doc = LegalDocumentBuilder.builder()
        .documentType(LegalDocumentType.OTHER)
        .build();

    assertThat(doc.getDocumentType()).isEqualTo(LegalDocumentType.OTHER);
    assertThat(doc.getFileName()).isNull();
    assertThat(doc.getObjectId()).isNull();
    assertThat(doc.getDescription()).isNull();
  }

  @Test
  void uploadedNow_setsOrOmitsUploadedAt() {
    LegalDocument withTimestamp = LegalDocumentBuilder.builder()
        .documentType(LegalDocumentType.ANNUAL_REPORT)
        .uploadedNow()
        .build();
    assertThat(withTimestamp.getUploadedAt()).isNotBlank();
    assertThat(withTimestamp.getUploadedAt()).matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}");

    LegalDocument withoutTimestamp = LegalDocumentBuilder.builder()
        .documentType(LegalDocumentType.CONTRACT)
        .build();
    assertThat(withoutTimestamp.getUploadedAt()).isNull();
  }
}
