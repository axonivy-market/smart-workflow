package com.axonivy.utils.smart.workflow.demo.erp.supplier.bean;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

import com.axonivy.utils.smart.workflow.demo.erp.department.repository.DepartmentRepository;
import com.axonivy.utils.smart.workflow.demo.erp.mock.MockDataGenerator;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.model.RuleType;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.repository.SupplierPolicyRuleRepository;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.repository.SupplierRepository;

import ch.ivyteam.ivy.cm.ContentObject;
import ch.ivyteam.ivy.cm.ContentObjectValue;
import ch.ivyteam.ivy.environment.Ivy;

@ManagedBean
@ViewScoped
public class StartDemoBean implements Serializable {

  private static final long serialVersionUID = 1L;

  private String step1Status = "pending";
  private String step2Status = "pending";
  private String step3Status = "pending";
  private boolean generationStarted = false;
  private boolean generationDone = false;

  @PostConstruct
  public void init() {
    if (isDataAlreadyGenerated()) {
      generationDone = true;
    }
  }

  public boolean isDataAlreadyGenerated() {
    var rules = SupplierPolicyRuleRepository.getInstance().findAll();
    boolean hasPolicyRules    = rules.stream().anyMatch(r -> r.getRuleType() == RuleType.POLICY);
    boolean hasFinancialRules = rules.stream().anyMatch(r -> r.getRuleType() == RuleType.FINANCIAL);
    boolean hasSuppliers      = !SupplierRepository.getInstance().findAll().isEmpty();
    return hasPolicyRules && hasFinancialRules && hasSuppliers;
  }

  // ── Step actions (invoked by p:remoteCommand) ─────────────────────────────

  public void generateVectorStore() {
    generationStarted = true;
    step1Status = "running";
    try {
      MockDataGenerator.ingestToVectorStore();
      step1Status = "completed";
    } catch (Exception e) {
      step1Status = "failed";
      Ivy.log().warn("Vector store ingestion failed (non-fatal, proceeding)", e);
    }
  }

  public void generateCompliancePolicies() {
    step2Status = "running";
    try {
      if (SupplierPolicyRuleRepository.getInstance().findAll().stream()
          .noneMatch(r -> r.getRuleType() == RuleType.POLICY)) {
        MockDataGenerator.generateCompliancePolicyRules();
      }
      step2Status = "completed";
    } catch (Exception e) {
      step2Status = "failed";
      Ivy.log().error("Compliance policy generation failed", e);
    }
  }

  public void generateFinancialPolicies() {
    step3Status = "running";
    try {
      if (SupplierPolicyRuleRepository.getInstance().findAll().stream()
          .noneMatch(r -> r.getRuleType() == RuleType.FINANCIAL)) {
        MockDataGenerator.generateFinancialPolicyRules();
      }
      if (DepartmentRepository.getInstance().findAll().isEmpty()) {
        MockDataGenerator.generateUsers();
        MockDataGenerator.generateDepartments();
      }
      if (SupplierRepository.getInstance().findAll().isEmpty()) {
        MockDataGenerator.generateSuppliers();
      }
      step3Status = "completed";
    } catch (Exception e) {
      step3Status = "failed";
      Ivy.log().error("Financial policy and supplier generation failed", e);
    }
    generationDone = true;
  }

  // ── CSS helpers for the timeline ──────────────────────────────────────────

  public String getStepClass(String status) {
    return switch (status) {
      case "running"   -> "so-checklist-item running so-tl-item";
      case "completed" -> "so-checklist-item completed so-tl-item";
      case "failed"    -> "so-checklist-item failed so-tl-item";
      default          -> "so-checklist-item pending so-tl-item";
    };
  }

  public String getBubbleClass(String status) {
    return switch (status) {
      case "running"   -> "so-tl-bubble so-tl-bubble-running";
      case "completed" -> "so-tl-bubble so-tl-bubble-completed";
      case "failed"    -> "so-tl-bubble so-tl-bubble-failed";
      default          -> "so-tl-bubble so-tl-bubble-pending";
    };
  }

  public String getStatusIcon(String status) {
    return switch (status) {
      case "running"   -> "ti ti-loader so-spin";
      case "completed" -> "ti ti-circle-check";
      case "failed"    -> "ti ti-circle-x";
      default          -> "ti ti-clock";
    };
  }

  // ── CMS file download ─────────────────────────────────────────────────────

  public StreamedContent downloadCmsFile(String cmsPath, String fileName) {
    ContentObjectValue contentObjectValue = loadFromCms(cmsPath);

    if (contentObjectValue == null) {
      Ivy.log().error("CMS file not found: " + cmsPath);
      return null;
    }

    return DefaultStreamedContent.builder()
        .name(fileName)
        .contentType(contentObjectValue.parent().meta().fileContentType().name())
        .stream(() -> contentObjectValue.read().inputStream())
        .build();
  }

  private static ContentObjectValue  loadFromCms(String cmsPath) {
    Optional<ContentObject> contentObject = Ivy.cm().findObject(cmsPath);
    if (!contentObject.map(ContentObject::exists).orElse(false)) {
      return null;
    }
    return contentObject.map(ContentObject::values)
                        .map(values -> values.getFirst()).get();
  }

  // ── Download all demo documents as ZIP ───────────────────────────────────

  private static final List<String[]> DEMO_DOCS = List.of(
      new String[]{"OfferLetter.pdf",              "/Files/ERP/DemoCompany/OfferLetter"},
      new String[]{"ISO9001_QualityManagement.pdf",       "/Files/ERP/DemoCompany/Certifications/ISO9001"},
      new String[]{"ISO14001_EnvironmentalManagement.pdf","/Files/ERP/DemoCompany/Certifications/ISO14001"},
      new String[]{"ISO27001_InformationSecurity.pdf",    "/Files/ERP/DemoCompany/Certifications/ISO27001"},
      new String[]{"GDPR_DataProcessingAgreement.pdf",    "/Files/ERP/DemoCompany/Certifications/GDPR"},
      new String[]{"Company_Registration_Extract.pdf",              "/Files/ERP/DemoCompany/LegalDocuments/CommercialRegister"},
      new String[]{"Self_Declaration.pdf",                 "/Files/ERP/DemoCompany/LegalDocuments/SelfDeclaration"},
      new String[]{"Annual_Report.pdf",                    "/Files/ERP/DemoCompany/LegalDocuments/AnnualReport"},
      new String[]{"Banking_Statement.pdf",                "/Files/ERP/DemoCompany/LegalDocuments/BankingStatement"}
  );

  public StreamedContent downloadAllDocuments() {
    try {
      var baos = new ByteArrayOutputStream();
      try (var zos = new ZipOutputStream(baos)) {
        for (var doc : DEMO_DOCS) {
          ContentObjectValue cov = loadFromCms(doc[1]);
          if (cov == null) continue;
          try (var is = cov.read().inputStream()) {
            zos.putNextEntry(new ZipEntry(doc[0]));
            is.transferTo(zos);
            zos.closeEntry();
          }
        }
      }
      var zipBytes = baos.toByteArray();
      return DefaultStreamedContent.builder()
          .name("Demo-Documents.zip")
          .contentType("application/zip")
          .stream(() -> new ByteArrayInputStream(zipBytes))
          .build();
    } catch (IOException e) {
      Ivy.log().error("Failed to create demo documents ZIP", e);
      return null;
    }
  }

  // ── Role guard ────────────────────────────────────────────────────────────

  public boolean isHasProcurementRole() {
    var user = Ivy.session().getSessionUser();
    if (user == null) {
      return false;
    }
    return user.getAllRoles().stream()
        .anyMatch(r -> "Procurement".equals(r.getName()));
  }

  // ── Getters ───────────────────────────────────────────────────────────────

  public String getStep1Status() { return step1Status; }
  public String getStep2Status() { return step2Status; }
  public String getStep3Status() { return step3Status; }
  public boolean isGenerationStarted() { return generationStarted; }
  public boolean isGenerationDone() { return generationDone; }
}
