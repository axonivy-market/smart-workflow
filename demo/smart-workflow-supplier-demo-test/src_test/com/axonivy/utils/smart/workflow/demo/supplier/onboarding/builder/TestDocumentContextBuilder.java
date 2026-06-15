package com.axonivy.utils.smart.workflow.demo.supplier.onboarding.builder;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.axonivy.utils.smart.workflow.demo.document.LegalDocument;
import com.axonivy.utils.smart.workflow.demo.document.LegalDocumentBuilder;
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
    LegalDocument withDesc = LegalDocumentBuilder.of(LegalDocumentType.CONTRACT, "contract.pdf", "Main supplier contract");
    assertThat(DocumentContextBuilder.of(withDesc).build())
        .contains("File: contract.pdf", "Type: CONTRACT", "Description: Main supplier contract");

    LegalDocument noDesc = LegalDocumentBuilder.of(LegalDocumentType.OTHER, "doc.pdf");
    assertThat(DocumentContextBuilder.of(noDesc).build()).contains("Description: n/a");
  }

  @Test
  void build_textContentIncluded_binaryExcluded() {
    LegalDocument text = LegalDocumentBuilder.of(LegalDocumentType.ANNUAL_REPORT, "report.pdf",
        "Annual financial results 2024".getBytes(StandardCharsets.UTF_8));
    assertThat(DocumentContextBuilder.of(text).build()).contains("Content: Annual financial results 2024");

    byte[] binaryContent = new byte[100];
    for (int i = 0; i < 100; i++) {
      binaryContent[i] = (byte) (i % 10 + 1);
    }
    LegalDocument binary = LegalDocumentBuilder.of(LegalDocumentType.OTHER, "binary.bin", binaryContent);
    assertThat(DocumentContextBuilder.of(binary).build()).doesNotContain("Content:");
  }

  @Test
  void build_contentTruncation_exceedsAndWithinLimit() {
    LegalDocument large = LegalDocumentBuilder.of(LegalDocumentType.OTHER, "large.txt",
        "ABCDEFGHIJ".getBytes(StandardCharsets.UTF_8));
    String truncated = DocumentContextBuilder.of(large).withContentLimit(5).build();
    assertThat(truncated).contains("ABCDE", "...[truncated]");

    LegalDocument small = LegalDocumentBuilder.of(LegalDocumentType.OTHER, "short.txt",
        "Hi".getBytes(StandardCharsets.UTF_8));
    assertThat(DocumentContextBuilder.of(small).build())
        .contains("Content: Hi").doesNotContain("...[truncated]");
  }

  @Test
  void withCertificationType_certificationAndNonCertificationDoc() {
    LegalDocument cert = LegalDocumentBuilder.of(LegalDocumentType.CERTIFICATION, "cert.pdf", "ISO 9001");
    assertThat(DocumentContextBuilder.of(cert).withCertificationType().build())
        .contains("CertificationType: ISO 9001").doesNotContain("Description:");

    LegalDocument nonCert = LegalDocumentBuilder.of(LegalDocumentType.COMMERCIAL_REGISTER, "register.pdf", "HRB 12345");
    assertThat(DocumentContextBuilder.of(nonCert).withCertificationType().build())
        .contains("Description: HRB 12345").doesNotContain("CertificationType:");
  }
}
