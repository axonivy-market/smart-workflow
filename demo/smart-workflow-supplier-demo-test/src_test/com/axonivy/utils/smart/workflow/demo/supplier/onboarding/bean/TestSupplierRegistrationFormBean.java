package com.axonivy.utils.smart.workflow.demo.supplier.onboarding.bean;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.primefaces.model.StreamedContent;

import com.axonivy.utils.smart.workflow.demo.assistant.AgentGuidance;
import com.axonivy.utils.smart.workflow.demo.assistant.AssistantChatMessage;
import com.axonivy.utils.smart.workflow.demo.common.Address;
import com.axonivy.utils.smart.workflow.demo.document.LegalDocument;
import com.axonivy.utils.smart.workflow.demo.document.enums.LegalDocumentType;
import com.axonivy.utils.smart.workflow.demo.supplier.Supplier;
import com.axonivy.utils.smart.workflow.demo.supplier.SupplierBanking;
import com.axonivy.utils.smart.workflow.demo.supplier.SupplierContact;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.OnboardingRequest;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.ValidationFinding;

import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
class TestSupplierRegistrationFormBean {

  @Test
  void init_whenSupplierIsMissing_createNestedObjects() {
    SupplierRegistrationFormBean bean = new SupplierRegistrationFormBean();
    OnboardingRequest request = new OnboardingRequest();

    bean.init(request);

    Supplier supplier = bean.getSupplier();

    assertThat(supplier).isNotNull();
    assertThat(supplier.getBusinessAddress()).isInstanceOf(Address.class);
    assertThat(supplier.getPrimaryContact()).isInstanceOf(SupplierContact.class);
    assertThat(supplier.getBanking()).isInstanceOf(SupplierBanking.class);
    assertThat(supplier.getCertifications()).isNotNull();
    assertThat(bean.getCountries()).isNotEmpty();
    assertThat(bean.getLegalForms()).isNotEmpty();
  }

  @Test
  void init_whenCalledTwice_initializeOnlyOnce() {
    SupplierRegistrationFormBean bean = new SupplierRegistrationFormBean();

    OnboardingRequest first = new OnboardingRequest();
    Supplier supplier = new Supplier();
    supplier.setSupplierId("SUP-001");
    first.setSupplier(supplier);

    bean.init(first);

    OnboardingRequest second = new OnboardingRequest();
    bean.init(second);

    assertThat(bean.getRequest()).isSameAs(first);
  }

  @Test
  void ensureObjectId_whenSupplierIdMissing_generateId() {
    SupplierRegistrationFormBean bean = new SupplierRegistrationFormBean();

    OnboardingRequest request = new OnboardingRequest();
    request.setSupplier(new Supplier());

    bean.init(request);

    String id = bean.ensureObjectId();

    assertThat(id).isNotBlank();
    assertThat(bean.getSupplier().getSupplierId()).isEqualTo(id);
  }

  @Test
  void ensureObjectId_whenSupplierIdExists_keepExistingId() {
    SupplierRegistrationFormBean bean = new SupplierRegistrationFormBean();

    Supplier supplier = new Supplier();
    supplier.setSupplierId("SUP-001");

    OnboardingRequest request = new OnboardingRequest();
    request.setSupplier(supplier);

    bean.init(request);

    assertThat(bean.ensureObjectId()).isEqualTo("SUP-001");
  }

  @Test
  void onDocumentSaved_whenCertificationDocument_addCertificationDocumentId() {
    SupplierRegistrationFormBean bean = new SupplierRegistrationFormBean();

    OnboardingRequest request = new OnboardingRequest();
    request.setSupplier(new Supplier());

    bean.init(request);

    LegalDocument doc = new LegalDocument();
    doc.setDocumentId("DOC-1");
    doc.setDocumentType(LegalDocumentType.ISO_9001);

    bean.onDocumentSaved(doc);

    assertThat(bean.getSupplier().getCertificationDocumentIds())
        .containsExactly("DOC-1");
  }

  @Test
  void onDocumentSaved_whenRequiredDocument_addRequiredDocumentIdOnlyOnce() {
    SupplierRegistrationFormBean bean = new SupplierRegistrationFormBean();

    OnboardingRequest request = new OnboardingRequest();
    request.setSupplier(new Supplier());

    bean.init(request);

    LegalDocument doc = new LegalDocument();
    doc.setDocumentId("DOC-1");
    doc.setDocumentType(LegalDocumentType.COMMERCIAL_REGISTER);

    bean.onDocumentSaved(doc);
    bean.onDocumentSaved(doc);

    assertThat(bean.getSupplier().getRequiredDocumentIds())
        .containsExactly("DOC-1");
  }

  @Test
  void onDocumentDeleted_whenDocumentsExist_removeDocumentIds() {
    SupplierRegistrationFormBean bean = new SupplierRegistrationFormBean();

    Supplier supplier = new Supplier();
    supplier.setCertificationDocumentIds(new ArrayList<>(List.of("CERT-1")));
    supplier.setRequiredDocumentIds(new ArrayList<>(List.of("DOC-1")));

    OnboardingRequest request = new OnboardingRequest();
    request.setSupplier(supplier);

    bean.init(request);

    LegalDocument cert = new LegalDocument();
    cert.setDocumentId("CERT-1");
    cert.setDocumentType(LegalDocumentType.ISO_9001);

    LegalDocument required = new LegalDocument();
    required.setDocumentId("DOC-1");
    required.setDocumentType(LegalDocumentType.COMMERCIAL_REGISTER);

    bean.onDocumentDeleted(cert);
    bean.onDocumentDeleted(required);

    assertThat(bean.getSupplier().getCertificationDocumentIds()).isEmpty();
    assertThat(bean.getSupplier().getRequiredDocumentIds()).isEmpty();
  }

  @Test
  void getDocumentByTypeKey_whenDocumentExists_returnMatchingDocument() {
    SupplierRegistrationFormBean bean = new SupplierRegistrationFormBean();

    OnboardingRequest request = new OnboardingRequest();
    request.setSupplier(new Supplier());

    bean.init(request);

    LegalDocument doc = new LegalDocument();
    doc.setDocumentType(LegalDocumentType.COMMERCIAL_REGISTER);

    bean.setSupplierDocuments(List.of(doc));

    assertThat(bean.getDocumentByTypeKey("COMMERCIAL_REGISTER"))
        .isSameAs(doc);
  }

  @Test
  void getDocumentByTypeKey_whenUnknownType_returnNull() {
    SupplierRegistrationFormBean bean = new SupplierRegistrationFormBean();

    OnboardingRequest request = new OnboardingRequest();
    request.setSupplier(new Supplier());

    bean.init(request);

    assertThat(bean.getDocumentByTypeKey("UNKNOWN"))
        .isNull();
  }

  @Test
  void downloadDocument_whenDocumentExists_returnStreamedContent() {
    SupplierRegistrationFormBean bean = new SupplierRegistrationFormBean();

    OnboardingRequest request = new OnboardingRequest();
    request.setSupplier(new Supplier());

    bean.init(request);

    LegalDocument doc = new LegalDocument();
    doc.setDocumentId("DOC-1");
    doc.setFileName("contract.pdf");
    doc.setFileContent("test".getBytes());

    bean.setSupplierDocuments(List.of(doc));

    StreamedContent content = bean.downloadDocument("DOC-1");

    assertThat(content).isNotNull();
    assertThat(content.getName()).isEqualTo("contract.pdf");
  }

  @Test
  void downloadDocument_whenDocumentMissing_returnNull() {
    SupplierRegistrationFormBean bean = new SupplierRegistrationFormBean();

    OnboardingRequest request = new OnboardingRequest();
    request.setSupplier(new Supplier());

    bean.init(request);

    assertThat(bean.downloadDocument("UNKNOWN")).isNull();
  }

  @Test
  void getPolicyValidationFindings_whenNullAndPresent_returnExpectedResult() {
    SupplierRegistrationFormBean bean = new SupplierRegistrationFormBean();

    OnboardingRequest request = new OnboardingRequest();
    request.setSupplier(new Supplier());

    bean.init(request);

    assertThat(bean.getPolicyValidationFindings()).isEmpty();

    ValidationFinding finding = new ValidationFinding();
    request.setPolicyValidationFindings(List.of(finding));

    assertThat(bean.getPolicyValidationFindings())
        .containsExactly(finding);
  }

  @Test
  void assistantProperties_whenUpdated_returnAssignedValues() {
    SupplierRegistrationFormBean bean = new SupplierRegistrationFormBean();

    bean.setAssistantUploadedFileName("supplier.md");
    bean.setAssistantUploadedContent("content");
    bean.setAssistantAwaitingConfirmation(true);
    bean.setAssistantParseFeedback("parsed");
    bean.setAgentUserMessage("hello");

    List<AssistantChatMessage> history = List.of(new AssistantChatMessage());
    bean.setAgentChatHistory(history);

    assertThat(bean.getAssistantUploadedFileName()).isEqualTo("supplier.md");
    assertThat(bean.getAssistantUploadedContent()).isEqualTo("content");
    assertThat(bean.getAssistantAwaitingConfirmation()).isTrue();
    assertThat(bean.getAssistantParseFeedback()).isEqualTo("parsed");
    assertThat(bean.getAgentUserMessage()).isEqualTo("hello");
    assertThat(bean.getAgentChatHistory()).isSameAs(history);
  }

  @Test
  void getAgentGuidance_returnExpectedGuidanceEntries() {
    SupplierRegistrationFormBean bean = new SupplierRegistrationFormBean();

    List<AgentGuidance> guidance = bean.getAgentGuidance();

    assertThat(guidance).hasSize(6);
    assertThat(guidance)
        .extracting(AgentGuidance::getQuestionPattern)
        .contains(
            "What certifications are required?",
            "How does risk scoring work?",
            "What documents do I need to upload?",
            "Can you parse my supplier document?");
  }
}