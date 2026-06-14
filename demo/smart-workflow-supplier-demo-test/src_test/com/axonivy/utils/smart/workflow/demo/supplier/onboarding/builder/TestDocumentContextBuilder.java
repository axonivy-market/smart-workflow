package com.axonivy.utils.smart.workflow.demo.supplier.onboarding.builder;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.axonivy.utils.smart.workflow.demo.document.LegalDocument;
import com.axonivy.utils.smart.workflow.demo.document.enums.LegalDocumentType;

import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
class TestDocumentContextBuilder {

  @Test
  void build_nullDoc_returnsNoDocumentMsg() {
    assertThat(DocumentContextBuilder.of(null).build()).isEqualTo("No document provided.");
  }

  @Test
  void build_basicFields_withAndWithoutDescription() {
    LegalDocument withDesc = new LegalDocument();
    withDesc.setFileName("contract.pdf");
    withDesc.setDocumentType(LegalDocumentType.CONTRACT);
    withDesc.setDescription("Main supplier contract");
    assertThat(DocumentContextBuilder.of(withDesc).build())
        .contains("File: contract.pdf", "Type: CONTRACT", "Description: Main supplier contract");

    LegalDocument noDesc = new LegalDocument();
    noDesc.setFileName("doc.pdf");
    noDesc.setDocumentType(LegalDocumentType.OTHER);
    assertThat(DocumentContextBuilder.of(noDesc).build()).contains("Description: n/a");
  }

  @Test
  void build_textContentIncluded_binaryExcluded() {
    LegalDocument text = new LegalDocument();
    text.setFileName("report.pdf");
    text.setDocumentType(LegalDocumentType.ANNUAL_REPORT);
    text.setFileContent("Annual financial results 2024".getBytes(StandardCharsets.UTF_8));
    assertThat(DocumentContextBuilder.of(text).build()).contains("Content: Annual financial results 2024");

    LegalDocument binary = new LegalDocument();
    binary.setFileName("binary.bin");
    binary.setDocumentType(LegalDocumentType.OTHER);
    byte[] binaryContent = new byte[100];
    for (int i = 0; i < 100; i++) {
      binaryContent[i] = (byte) (i % 10 + 1);
    }
    binary.setFileContent(binaryContent);
    assertThat(DocumentContextBuilder.of(binary).build()).doesNotContain("Content:");
  }

  @Test
  void build_contentTruncation_exceedsAndWithinLimit() {
    LegalDocument large = new LegalDocument();
    large.setFileName("large.txt");
    large.setDocumentType(LegalDocumentType.OTHER);
    large.setFileContent("ABCDEFGHIJ".getBytes(StandardCharsets.UTF_8));
    String truncated = DocumentContextBuilder.of(large).withContentLimit(5).build();
    assertThat(truncated).contains("ABCDE", "...[truncated]");

    LegalDocument small = new LegalDocument();
    small.setFileName("short.txt");
    small.setDocumentType(LegalDocumentType.OTHER);
    small.setFileContent("Hi".getBytes(StandardCharsets.UTF_8));
    assertThat(DocumentContextBuilder.of(small).build())
        .contains("Content: Hi").doesNotContain("...[truncated]");
  }

  @Test
  void withCertificationType_certificationAndNonCertificationDoc() {
    LegalDocument cert = new LegalDocument();
    cert.setFileName("cert.pdf");
    cert.setDocumentType(LegalDocumentType.CERTIFICATION);
    cert.setDescription("ISO 9001");
    assertThat(DocumentContextBuilder.of(cert).withCertificationType().build())
        .contains("CertificationType: ISO 9001").doesNotContain("Description:");

    LegalDocument nonCert = new LegalDocument();
    nonCert.setFileName("register.pdf");
    nonCert.setDocumentType(LegalDocumentType.COMMERCIAL_REGISTER);
    nonCert.setDescription("HRB 12345");
    assertThat(DocumentContextBuilder.of(nonCert).withCertificationType().build())
        .contains("Description: HRB 12345").doesNotContain("CertificationType:");
  }
}
