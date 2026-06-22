package com.axonivy.utils.smart.workflow.demo.supplier.onboarding.bean;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

import com.axonivy.utils.smart.workflow.demo.department.DepartmentRepository;
import com.axonivy.utils.smart.workflow.demo.employee.EmployeeRepository;
import com.axonivy.utils.smart.workflow.demo.mock.MockRules;
import com.axonivy.utils.smart.workflow.demo.mock.SupplierOnboardingKnowledge;
import com.axonivy.utils.smart.workflow.demo.mock.VectorStoreRestClient;
import com.axonivy.utils.smart.workflow.demo.supplier.SupplierPolicyRule;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.enums.RuleType;
import com.axonivy.utils.smart.workflow.demo.supplier.repository.SupplierPolicyRuleRepository;
import com.axonivy.utils.smart.workflow.demo.supplier.repository.SupplierRepository;
import com.axonivy.utils.smart.workflow.rag.opensearch.internal.OpenSearchRestClient;
import com.axonivy.utils.smart.workflow.rag.pipeline.internal.OpenSearchIngestor;

import ch.ivyteam.ivy.cm.ContentObject;
import ch.ivyteam.ivy.cm.ContentObjectValue;
import ch.ivyteam.ivy.environment.Ivy;

@ManagedBean
@ViewScoped
public class StartDemoBean implements Serializable {

  private static final long serialVersionUID = 1L;

  public enum StepStatus {
    PENDING   ("so-checklist-item pending so-tl-item",   "so-tl-bubble so-tl-bubble-pending",   "ti ti-clock"),
    RUNNING   ("so-checklist-item running so-tl-item",   "so-tl-bubble so-tl-bubble-running",   "ti ti-loader so-spin"),
    COMPLETED ("so-checklist-item completed so-tl-item", "so-tl-bubble so-tl-bubble-completed", "ti ti-circle-check"),
    FAILED    ("so-checklist-item failed so-tl-item",    "so-tl-bubble so-tl-bubble-failed",    "ti ti-circle-x");

    private final String stepClass;
    private final String bubbleClass;
    private final String statusIcon;

    StepStatus(String stepClass, String bubbleClass, String statusIcon) {
      this.stepClass   = stepClass;
      this.bubbleClass = bubbleClass;
      this.statusIcon  = statusIcon;
    }

    public String getStepClass()   { return stepClass; }
    public String getBubbleClass() { return bubbleClass; }
    public String getStatusIcon()  { return statusIcon; }
  }

  private static final String ROLE_PROCUREMENT       = "Procurement";
  private static final String ZIP_FILE_NAME          = "Demo-Documents.zip";
  private static final String ZIP_CONTENT_TYPE       = "application/zip";
  private static final String LOG_INDEX_DELETED      = "Deleted existing vector store index '%s' before re-ingestion.";
  private static final String LOG_CHUNK_INGESTED     = "Ingested policy chunk %d/%d into '%s'.";
  private static final String LOG_CMS_NOT_FOUND      = "CMS file not found: %s";

  private enum DemoDoc {
    OFFER_LETTER                  ("OfferLetter.pdf",                      "/Files/ERP/DemoCompany/OfferLetter"),
    ISO9001_QUALITY_MANAGEMENT    ("ISO9001_QualityManagement.pdf",        "/Files/ERP/DemoCompany/Certifications/ISO9001"),
    ISO14001_ENVIRONMENTAL        ("ISO14001_EnvironmentalManagement.pdf", "/Files/ERP/DemoCompany/Certifications/ISO14001"),
    ISO27001_INFORMATION_SECURITY ("ISO27001_InformationSecurity.pdf",     "/Files/ERP/DemoCompany/Certifications/ISO27001"),
    GDPR_DATA_PROCESSING          ("GDPR_DataProcessingAgreement.pdf",     "/Files/ERP/DemoCompany/Certifications/GDPR"),
    COMPANY_REGISTRATION          ("Company_Registration_Extract.pdf",     "/Files/ERP/DemoCompany/LegalDocuments/CommercialRegister"),
    SELF_DECLARATION              ("Self_Declaration.pdf",                 "/Files/ERP/DemoCompany/LegalDocuments/SelfDeclaration"),
    ANNUAL_REPORT                 ("Annual_Report.pdf",                    "/Files/ERP/DemoCompany/LegalDocuments/AnnualReport"),
    BANKING_STATEMENT             ("Banking_Statement.pdf",                "/Files/ERP/DemoCompany/LegalDocuments/BankingStatement");

    private final String fileName;
    private final String cmsPath;

    DemoDoc(String fileName, String cmsPath) {
      this.fileName = fileName;
      this.cmsPath  = cmsPath;
    }

    String getFileName() { return fileName; }

    ContentObjectValue getCms() {
      return loadFromCms(cmsPath);
    }
  }

  private StepStatus step1Status = StepStatus.PENDING;
  private StepStatus step2Status = StepStatus.PENDING;
  private StepStatus step3Status = StepStatus.PENDING;
  private boolean generationStarted = false;
  private boolean generationDone = false;

  private List<KnowledgeChunkItem> knowledgeChunks;
  private List<SupplierPolicyRule> complianceRules;
  private List<SupplierPolicyRule> financialRules;

  public static class KnowledgeChunkItem implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String title;
    private final String body;

    KnowledgeChunkItem(String title, String body) {
      this.title = title;
      this.body  = body;
    }

    public String getTitle() { return title; }
    public String getBody()  { return body; }
  }

  @PostConstruct
  public void init() {
    knowledgeChunks = buildKnowledgeChunks();
    complianceRules = buildComplianceRules();
    financialRules  = buildFinancialRules();
    if (isDataAlreadyGenerated()) {
      generationDone = true;
    }
  }

  private static List<KnowledgeChunkItem> buildKnowledgeChunks() {
    List<KnowledgeChunkItem> items = new ArrayList<>();
    for (String chunk : SupplierOnboardingKnowledge.getAll()) {
      int nl = chunk.indexOf('\n');
      String title = nl > 0 ? chunk.substring(0, nl) : chunk;
      if (title.endsWith(":")) title = title.substring(0, title.length() - 1);
      String body  = nl > 0 ? chunk.substring(nl + 1) : "";
      items.add(new KnowledgeChunkItem(title, body));
    }
    return items;
  }

  private static List<SupplierPolicyRule> buildComplianceRules() {
    return Arrays.stream(MockRules.values())
        .filter(r -> r.ruleType() == RuleType.POLICY)
        .map(StartDemoBean::toRule)
        .toList();
  }

  private static List<SupplierPolicyRule> buildFinancialRules() {
    return Arrays.stream(MockRules.values())
        .filter(r -> r.ruleType() == RuleType.FINANCIAL)
        .map(StartDemoBean::toRule)
        .toList();
  }

  private static SupplierPolicyRule toRule(MockRules mockRule) {
    SupplierPolicyRule r = new SupplierPolicyRule();
    r.setTarget(mockRule.name());
    r.setRule(mockRule.rule());
    r.setRiskScore(mockRule.riskScore());
    r.setLegalDocumentType(mockRule.docType());
    return r;
  }

  public String formatIndex(int i) {
    return i < 10 ? "0" + i : String.valueOf(i);
  }

  public boolean isDataAlreadyGenerated() {
    var rules = SupplierPolicyRuleRepository.getInstance().findAll();
    boolean hasPolicyRules    = rules.stream().anyMatch(r -> r.getRuleType() == RuleType.POLICY);
    boolean hasFinancialRules = rules.stream().anyMatch(r -> r.getRuleType() == RuleType.FINANCIAL);
    boolean hasSuppliers      = !SupplierRepository.getInstance().findAll().isEmpty();
    return hasPolicyRules && hasFinancialRules && hasSuppliers;
  }

  public void generateVectorStore() {
    generationStarted = true;
    step1Status = StepStatus.RUNNING;
    try {
      String collection = SupplierOnboardingKnowledge.COLLECTION;
      OpenSearchRestClient client = OpenSearchRestClient.fromIvyVars();
      if (client.indexExists(collection)) {
        VectorStoreRestClient.deleteIndex(collection);
        Ivy.log().info(String.format(LOG_INDEX_DELETED, collection));
      }
      OpenSearchIngestor ingestor = new OpenSearchIngestor();
      List<String> chunks = SupplierOnboardingKnowledge.getAll();
      for (int i = 0; i < chunks.size(); i++) {
        ingestor.ingest(collection, List.of(chunks.get(i)));
        Ivy.log().info(String.format(LOG_CHUNK_INGESTED, i + 1, chunks.size(), collection));
      }
      step1Status = StepStatus.COMPLETED;
    } catch (Exception e) {
      step1Status = StepStatus.FAILED;
      Ivy.log().warn("Vector store ingestion failed (non-fatal, proceeding)", e);
    }
  }

  public void generateCompliancePolicies() {
    step2Status = StepStatus.RUNNING;
    try {
      SupplierPolicyRuleRepository.getInstance().install();
      step2Status = StepStatus.COMPLETED;
    } catch (Exception e) {
      step2Status = StepStatus.FAILED;
      Ivy.log().error("Compliance policy generation failed", e);
    }
  }

  public void generateFinancialPolicies() {
    step3Status = StepStatus.RUNNING;
    try {
      EmployeeRepository.getInstance().install();
      DepartmentRepository.getInstance().install();
      SupplierRepository.getInstance().install();
      step3Status = StepStatus.COMPLETED;
    } catch (Exception e) {
      step3Status = StepStatus.FAILED;
      Ivy.log().error("Financial policy and supplier generation failed", e);
    }
    generationDone = true;
  }

  public StreamedContent downloadCmsFile(String cmsPath, String fileName) {
    ContentObjectValue contentObjectValue = loadFromCms(cmsPath);
    if (contentObjectValue == null) {
      Ivy.log().error(String.format(LOG_CMS_NOT_FOUND, cmsPath));
      return null;
    }
    return DefaultStreamedContent.builder()
        .name(fileName)
        .contentType(contentObjectValue.parent().meta().fileContentType().name())
        .stream(() -> contentObjectValue.read().inputStream())
        .build();
  }

  private static ContentObjectValue loadFromCms(String cmsPath) {
    Optional<ContentObject> contentObject = Ivy.cm().findObject(cmsPath);
    if (!contentObject.map(ContentObject::exists).orElse(false)) {
      return null;
    }
    return contentObject.map(ContentObject::values)
                        .filter(values -> !values.isEmpty())
                        .map(List::getFirst)
                        .orElse(null);
  }

  public StreamedContent downloadAllDocuments() {
    try {
      var zipBytes = buildDocumentsZip();
      return DefaultStreamedContent.builder()
          .name(ZIP_FILE_NAME)
          .contentType(ZIP_CONTENT_TYPE)
          .stream(() -> new ByteArrayInputStream(zipBytes))
          .build();
    } catch (IOException e) {
      Ivy.log().error("Failed to create demo documents ZIP", e);
      return null;
    }
  }

  private static byte[] buildDocumentsZip() throws IOException {
    var outputStream = new ByteArrayOutputStream();
    try (var zipStream = new ZipOutputStream(outputStream)) {
      for (DemoDoc doc : DemoDoc.values()) {
        ContentObjectValue contentObject = doc.getCms();
        if (contentObject != null) {
          try (var contentObjectInputStream = contentObject.read().inputStream()) {
            zipStream.putNextEntry(new ZipEntry(doc.getFileName()));
            contentObjectInputStream.transferTo(zipStream);
            zipStream.closeEntry();
          }
        }
      }
    }
    return outputStream.toByteArray();
  }

  public boolean isHasProcurementRole() {
    var user = Ivy.session().getSessionUser();
    if (user == null) {
      return false;
    }
    return user.getAllRoles().stream()
        .anyMatch(r -> ROLE_PROCUREMENT.equals(r.getName()));
  }

  public StepStatus getStep1Status() { return step1Status; }
  public StepStatus getStep2Status() { return step2Status; }
  public StepStatus getStep3Status() { return step3Status; }
  public boolean isGenerationStarted() { return generationStarted; }
  public boolean isGenerationDone() { return generationDone; }
  public List<KnowledgeChunkItem> getKnowledgeChunks() { return knowledgeChunks; }
  public List<SupplierPolicyRule> getComplianceRules() { return complianceRules; }
  public List<SupplierPolicyRule> getFinancialRules()  { return financialRules; }
}
