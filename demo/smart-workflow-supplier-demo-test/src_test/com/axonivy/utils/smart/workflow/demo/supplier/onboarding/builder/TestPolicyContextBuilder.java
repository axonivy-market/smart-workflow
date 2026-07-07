package com.axonivy.utils.smart.workflow.demo.supplier.onboarding.builder;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.axonivy.utils.smart.workflow.demo.document.enums.LegalDocumentType;
import com.axonivy.utils.smart.workflow.demo.supplier.SupplierPolicyRule;
import com.axonivy.utils.smart.workflow.demo.supplier.agent.DocumentExtractionResult;
import com.axonivy.utils.smart.workflow.demo.supplier.agent.ExtractedDoc;

import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
class TestPolicyContextBuilder {

  @Test
  void buildPolicyDocumentContext_nullOrEmpty_returnsNoDocumentsText() {
    assertThat(PolicyContextBuilder.buildPolicyDocumentContext(null))
        .contains("No documents were extracted.");

    DocumentExtractionResult empty = new DocumentExtractionResult();
    empty.setDocumentSummaries(new ArrayList<>());
    assertThat(PolicyContextBuilder.buildPolicyDocumentContext(empty))
        .contains("No documents were extracted.");
  }

  @Test
  void buildPolicyDocumentContext_docContentPresenceHandling() {
    DocumentExtractionResult withContent = new DocumentExtractionResult();
    ExtractedDoc doc = new ExtractedDoc();
    doc.setFileName("iso9001.pdf");
    doc.setDocumentType(LegalDocumentType.ISO_9001);
    doc.setContent("ISO 9001 quality management content");
    withContent.setDocumentSummaries(List.of(doc));
    assertThat(PolicyContextBuilder.buildPolicyDocumentContext(withContent))
        .contains("iso9001.pdf", "ISO 9001 quality management content");

    DocumentExtractionResult noContent = new DocumentExtractionResult();
    ExtractedDoc emptyDoc = new ExtractedDoc();
    emptyDoc.setFileName("empty.pdf");
    emptyDoc.setDocumentType(LegalDocumentType.OTHER);
    noContent.setDocumentSummaries(List.of(emptyDoc));
    assertThat(PolicyContextBuilder.buildPolicyDocumentContext(noContent))
        .contains("(no content extracted)");
  }

  @Test
  void buildRuleDocContext_noMatchingDoc_containsNoDocFoundText() {
    SupplierPolicyRule rule = new SupplierPolicyRule();
    rule.setTarget("RULE_01");
    rule.setRule("Supplier must have commercial register");
    rule.setRiskScore(20);
    rule.setLegalDocumentType(LegalDocumentType.COMMERCIAL_REGISTER);

    DocumentExtractionResult extraction = new DocumentExtractionResult();
    extraction.setDocumentSummaries(new ArrayList<>());

    assertThat(PolicyContextBuilder.buildRuleDocContext(rule, extraction, "Supplier context"))
        .contains("No specific document found for this rule.");
  }

  @Test
  void buildRuleDocContext_withMatchingDoc_containsDocDetails() {
    SupplierPolicyRule rule = new SupplierPolicyRule();
    rule.setTarget("CERT_RULE");
    rule.setRule("Must have ISO 9001");
    rule.setRiskScore(15);
    rule.setCertificationType(LegalDocumentType.ISO_9001);

    DocumentExtractionResult extraction = new DocumentExtractionResult();
    ExtractedDoc doc = new ExtractedDoc();
    doc.setFileName("iso9001_cert.pdf");
    doc.setDocumentType(LegalDocumentType.ISO_9001);
    doc.setContent("ISO 9001 certified since 2020");
    extraction.setDocumentSummaries(List.of(doc));

    assertThat(PolicyContextBuilder.buildRuleDocContext(rule, extraction, "Supplier context"))
        .contains("iso9001_cert.pdf", "ISO 9001 certified since 2020");
  }

  @Test
  void buildSingleRuleSystemPrompt_containsRuleDetails() {
    SupplierPolicyRule rule = new SupplierPolicyRule();
    rule.setTarget("VAT_RULE");
    rule.setRule("Must have valid VAT ID");
    rule.setRiskScore(10);

    assertThat(PolicyContextBuilder.buildSingleRuleSystemPrompt(rule))
        .contains("VAT_RULE", "Must have valid VAT ID", "10");
  }

  @Test
  void hasRuleDocument_allCases() {
    assertThat(PolicyContextBuilder.hasRuleDocument(new SupplierPolicyRule(), null)).isTrue();

    SupplierPolicyRule withCert = new SupplierPolicyRule();
    withCert.setCertificationType(LegalDocumentType.ISO_9001);
    DocumentExtractionResult matching = new DocumentExtractionResult();
    ExtractedDoc doc = new ExtractedDoc();
    doc.setDocumentType(LegalDocumentType.ISO_9001);
    matching.setDocumentSummaries(List.of(doc));
    assertThat(PolicyContextBuilder.hasRuleDocument(withCert, matching)).isTrue();

    SupplierPolicyRule withOtherCert = new SupplierPolicyRule();
    withOtherCert.setCertificationType(LegalDocumentType.ISO_27001);
    assertThat(PolicyContextBuilder.hasRuleDocument(withOtherCert, matching)).isFalse();
  }
}
