package com.axonivy.utils.smart.workflow.demo.erp.mock;

import java.util.Arrays;
import java.util.List;

import com.axonivy.utils.smart.workflow.demo.erp.department.model.Department;
import com.axonivy.utils.smart.workflow.demo.erp.department.repository.DepartmentRepository;
import com.axonivy.utils.smart.workflow.demo.erp.document.LegalDocument;
import com.axonivy.utils.smart.workflow.demo.erp.document.LegalDocumentObjectType;
import com.axonivy.utils.smart.workflow.demo.erp.document.LegalDocumentRepository;
import com.axonivy.utils.smart.workflow.demo.erp.document.LegalDocumentType;
import com.axonivy.utils.smart.workflow.demo.erp.employee.model.Employee;
import com.axonivy.utils.smart.workflow.demo.erp.employee.repository.EmployeeRepository;
import com.axonivy.utils.smart.workflow.demo.erp.rag.SupplierOnboardingKnowledge;
import com.axonivy.utils.smart.workflow.demo.erp.shared.Address;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.model.RuleType;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.model.Supplier;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.model.SupplierBanking;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.model.SupplierCertification;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.model.SupplierContact;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.model.SupplierPolicyRule;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.repository.SupplierPolicyRuleRepository;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.repository.SupplierRepository;
import com.axonivy.utils.smart.workflow.rag.pipeline.internal.OpenSearchIngestor;

import ch.ivyteam.ivy.environment.Ivy;

public class MockDataGenerator {

  public static void mock() {
    if (SupplierPolicyRuleRepository.getInstance().findAll().stream().noneMatch(r -> r.getRuleType() == RuleType.POLICY)) {
      generateCompliancePolicyRules();
    }
    if (SupplierPolicyRuleRepository.getInstance().findAll().stream().noneMatch(r -> r.getRuleType() == RuleType.FINANCIAL)) {
      generateFinancialPolicyRules();
    }
    if (DepartmentRepository.getInstance().findAll().isEmpty()) {
      generateUsers();
      generateDepartments();
    }
    if (SupplierRepository.getInstance().findAll().isEmpty()) {
      generateSuppliers();
    }
    ingestToVectorStore();
  }

  // ── Vector Store Ingestion ────────────────────────────────────────────────

  public static void ingestToVectorStore() {
    try {
      String collection = SupplierOnboardingKnowledge.COLLECTION;
      VectorStoreRestClient vsClient = VectorStoreRestClient.fromIvyVars();
      if (vsClient.indexExists(collection)) {
        vsClient.deleteIndex(collection);
        Ivy.log().info("Deleted existing vector store index '" + collection + "' before re-ingestion.");
      }
      OpenSearchIngestor ingestor = new OpenSearchIngestor();
      List<String> chunks = SupplierOnboardingKnowledge.getAll();
      for (int i = 0; i < chunks.size(); i++) {
        ingestor.ingest(collection, List.of(chunks.get(i)));
        Ivy.log().info("Ingested policy chunk " + (i + 1) + "/" + chunks.size() + " into '" + collection + "'.");
      }
    } catch (Exception ex) {
      Ivy.log().warn("Demo data vector store ingestion skipped — OpenSearch not available", ex);
    }
  }

    // ── Supplier Policy Rules ────────────────────────────────────────────────

  public static void generateCompliancePolicyRules() {
    Ivy.log().info("Generating compliance policy rules...");

    SupplierPolicyRuleRepository repo = SupplierPolicyRuleRepository.getInstance();
    repo.create(policyRule("RULE_03_ISO_9001",
      "If ISO 9001 certificate is uploaded, validate that it is not expired and the certificate number is properly formatted. Flag as WARNING if the cert expires within 6 months.", 20, LegalDocumentType.ISO_9001));
    repo.create(policyRule("RULE_04_ISO_14001",
      "If ISO 14001 certificate is uploaded, validate that it is not expired and the scope covers the supplier's stated manufacturing or industrial operations.", 20, LegalDocumentType.ISO_14001));
    repo.create(policyRule("RULE_05_ISO_27001",
      "If ISO 27001 certificate is uploaded, validate that it is not expired and the scope covers IT or data processing activities relevant to the supplier.", 20, LegalDocumentType.ISO_27001));
    repo.create(policyRule("RULE_06_GDPR_DPA",
      "If a GDPR Data Processing Agreement is uploaded, validate that it covers the supplier's data processing obligations and appears properly signed or executed.", 20, LegalDocumentType.GDPR_DPA));
    repo.create(policyRule("RULE_07_EU_EEA_PREFERENCE",
      "Suppliers outside EU/EEA should be flagged as warning.", 10));
    repo.create(policyRule("RULE_08_ANNUAL_REPORT_FINANCIAL_SOUNDNESS",
      "Annual report is recommended; if present, financial soundness checks must pass.", 15, LegalDocumentType.ANNUAL_REPORT));

    Ivy.log().info("Generated 6 compliance policy rules.");
  }

  public static void generateFinancialPolicyRules() {
    Ivy.log().info("Generating financial policy rules...");

    SupplierPolicyRuleRepository repo = SupplierPolicyRuleRepository.getInstance();
    repo.create(financialRule("FIN_RULE_03_NEGATIVE_EQUITY",
      "If the annual report indicates negative total equity or net liabilities exceeding total assets, flag as FAILURE.", 40, LegalDocumentType.ANNUAL_REPORT));
    repo.create(financialRule("FIN_RULE_04_OPERATING_LOSS",
      "If the annual report shows an operating loss for the reported fiscal year, flag as WARNING. Do not consider the report date or whether the report is current — only evaluate the financial content present in the document.", 20, LegalDocumentType.ANNUAL_REPORT));
    repo.create(financialRule("FIN_RULE_05_INSOLVENCY_PROCEEDINGS",
      "If the annual report or any submitted document mentions insolvency proceedings, administration, receivership, or a court-ordered asset freeze, flag as FAILURE.", 60, LegalDocumentType.ANNUAL_REPORT));
    repo.create(financialRule("FIN_RULE_06_COMPANY_DISSOLVED",
      "If the commercial register extract shows the company as dissolved, struck off, or in liquidation, flag as FAILURE.", 80, LegalDocumentType.COMMERCIAL_REGISTER));
    repo.create(financialRule("FIN_RULE_07_TAX_CERTIFICATE_EXPIRED",
      "If a tax certificate is uploaded and its validity date has passed, flag as WARNING. An expired tax certificate indicates unresolved compliance obligations.", 15, LegalDocumentType.TAX_CERTIFICATE));
    repo.create(financialRule("FIN_RULE_08_BANKING_ACCOUNT_MISMATCH",
      "If a banking confirmation is uploaded and the account holder name does not match the registered supplier legal name, flag as WARNING.", 20, LegalDocumentType.BANKING_CONFIRMATION));

    Ivy.log().info("Generated 6 financial policy rules.");
  }

  // ── Employees & Departments ───────────────────────────────────────────────

  public static void generateUsers() {
    Ivy.log().info("Generating mock employees...");

    EmployeeRepository repo = EmployeeRepository.getInstance();
    repo.create(employee("sandra.collins", "Sandra", "Collins", null,       "ProcurementDirector", "sandra.collins@buildrightco.com"));
    repo.create(employee("robert.hayes",   "Robert", "Hayes",   "DEPT-001", "DepartmentManager",   "robert.hayes@buildrightco.com"));
    repo.create(employee("karen.mitchell", "Karen",  "Mitchell","DEPT-002", "DepartmentManager",   "karen.mitchell@buildrightco.com"));
    repo.create(employee("james.thornton", "James",  "Thornton","DEPT-003", "DepartmentManager",   "james.thornton@buildrightco.com"));
    repo.create(employee("lisa.nguyen",    "Lisa",   "Nguyen",  "DEPT-004", "DepartmentManager",   "lisa.nguyen@buildrightco.com"));
    repo.create(employee("marcus.webb",    "Marcus", "Webb",    "DEPT-005", "DepartmentManager",   "marcus.webb@buildrightco.com"));
    repo.create(employee("david.chen",     "David",  "Chen",    "DEPT-001", "Procurement",         "david.chen@buildrightco.com"));
    repo.create(employee("emily.ross",     "Emily",  "Ross",    "DEPT-002", "Procurement",         "emily.ross@buildrightco.com"));
    repo.create(employee("tom.banks",      "Tom",    "Banks",   "DEPT-003", "Procurement",         "tom.banks@buildrightco.com"));
    repo.create(employee("claire.ford",    "Claire", "Ford",    "DEPT-004", "Procurement",         "claire.ford@buildrightco.com"));

    Ivy.log().info("Generated 10 employees for BuildRight Supply Co.");
  }

  public static void generateDepartments() {
    Ivy.log().info("Generating mock departments...");

    DepartmentRepository repo = DepartmentRepository.getInstance();
    repo.create(department("DEPT-001", "Lumber & Building Materials", "robert.hayes",   "sandra.collins"));
    repo.create(department("DEPT-002", "Electrical & Plumbing",       "karen.mitchell", "sandra.collins"));
    repo.create(department("DEPT-003", "Tools & Hardware",            "james.thornton", "sandra.collins"));
    repo.create(department("DEPT-004", "Flooring & Decor",            "lisa.nguyen",    "sandra.collins"));
    repo.create(department("DEPT-005", "Garden & Outdoor Living",     "marcus.webb",    "sandra.collins"));

    Ivy.log().info("Generated 5 departments for BuildRight Supply Co.");
  }

  // ── Suppliers ─────────────────────────────────────────────────────────────

  public static void generateSuppliers() {
    Ivy.log().info("Generating mock suppliers...");

    SupplierRepository repo = SupplierRepository.getInstance();

    // SUP-01 — Lumber & Building Materials (DEPT-001)
    Supplier sup01 = repo.create(supplier(
        "SUP-2019-0001", "Holzmann & Partner GmbH", "GmbH", "DE198765432", "HRB 67543",
        "Lumber & Building Materials",
        new Address("Holzweg 12", null, "Hamburg", null, "20095", "DE"),
        "+49 40 9876543", "info@holzmann-partner.de", "https://www.holzmann-partner.de",
        new SupplierContact("Klaus", "Holzmann", "CEO", "k.holzmann@holzmann-partner.de", "+49 40 9876543"),
        new SupplierBanking("DE12500105170648489890", "BELADEBEXXX", "Deutsche Bank"),
        Arrays.asList(
            new SupplierCertification(LegalDocumentType.ISO_9001, "DE-2021-00781", "2026-05-31", "iso9001_holzmann.pdf", true),
            new SupplierCertification(LegalDocumentType.ISO_14001, "DE-2021-00782", "2026-05-31", "iso14001_holzmann.pdf", true))));
    generateDocumentsForSupplier(sup01);

    // SUP-02 — Tools & Hardware — *** 80% match with TechVision GmbH ***
    // Same name root "TechVision", same country DE, same business purpose
    Supplier sup02 = repo.create(supplier(
        "SUP-2021-0002", "TechVision AG", "AG", "DE345678901", "HRB 112233",
        "Tools & Hardware",
        new Address("Industriestraße 7", null, "Munich", null, "80339", "DE"),
        "+49 89 3456789", "info@techvision-ag.de", "https://www.techvision-ag.de",
        new SupplierContact("Michael", "Bauer", "Managing Director", "m.bauer@techvision-ag.de", "+49 89 3456789"),
        new SupplierBanking("DE89370400440532013000", "COBADEFFXXX", "Commerzbank"),
        Arrays.asList(
            new SupplierCertification(LegalDocumentType.ISO_9001, "DE-2020-00412", "2026-03-15", "iso9001_techvision_ag.pdf", true))));
    generateDocumentsForSupplier(sup02);

    // SUP-03 — Tools & Hardware — *** 50% match with TechVision GmbH ***
    // Partial name match "Vision" + "Tool", same purpose, same country
    Supplier sup03 = repo.create(supplier(
        "SUP-2022-0003", "VisionTool Systems GmbH", "GmbH", "DE456789012", "HRB 223344",
        "Tools & Hardware",
        new Address("Werkzeugstraße 33", null, "Stuttgart", null, "70173", "DE"),
        "+49 711 5678901", "contact@visiontool.de", "https://www.visiontool.de",
        new SupplierContact("Anna", "Weber", "Sales Director", "a.weber@visiontool.de", "+49 711 5678901"),
        new SupplierBanking("DE21200400300002345678", "COBADEFFXXX", "HypoVereinsbank"),
        Arrays.asList(
            new SupplierCertification(LegalDocumentType.ISO_9001, "DE-2022-00567", "2025-12-31", "iso9001_visiontool.pdf", true))));
    generateDocumentsForSupplier(sup03);

    // SUP-04 — Electrical & Plumbing (DEPT-002)
    Supplier sup04 = repo.create(supplier(
        "SUP-2020-0004", "NordElektro GmbH", "GmbH", "DE567890123", "HRB 334455",
        "Electrical & Plumbing",
        new Address("Stromweg 5", null, "Berlin", null, "10115", "DE"),
        "+49 30 6789012", "info@nordelektro.de", "https://www.nordelektro.de",
        new SupplierContact("Stefan", "Klein", "CEO", "s.klein@nordelektro.de", "+49 30 6789012"),
        new SupplierBanking("DE75512108001245126199", "SSKMDEMM", "Stadtsparkasse"),
        Arrays.asList(
            new SupplierCertification(LegalDocumentType.ISO_9001, "DE-2020-00333", "2025-08-31", "iso9001_nordelektro.pdf", true),
            new SupplierCertification(LegalDocumentType.ISO_27001, "DE-2020-00334", "2025-08-31", "iso27001_nordelektro.pdf", true))));
    generateDocumentsForSupplier(sup04);

    // SUP-05 — Flooring & Decor (DEPT-004)
    Supplier sup05 = repo.create(supplier(
        "SUP-2021-0005", "AlpenFloor AG", "AG", "AT678901234", "FN 123456a",
        "Flooring & Decor",
        new Address("Parkettallee 18", null, "Vienna", null, "1010", "AT"),
        "+43 1 7890123", "office@alpenfloor.at", "https://www.alpenfloor.at",
        new SupplierContact("Eva", "Gruber", "Export Manager", "e.gruber@alpenfloor.at", "+43 1 7890123"),
        new SupplierBanking("AT483200000012345864", "RLNWATWWGD0", "Raiffeisenbank"),
        Arrays.asList(
            new SupplierCertification(LegalDocumentType.ISO_14001, "AT-2021-00210", "2026-09-30", "iso14001_alpenfloor.pdf", true))));
    generateDocumentsForSupplier(sup05);

    // SUP-06 — Garden & Outdoor Living (DEPT-005)
    Supplier sup06 = repo.create(supplier(
        "SUP-2023-0006", "GartenPlus GmbH", "GmbH", "DE789012345", "HRB 445566",
        "Garden & Outdoor Living",
        new Address("Gartenweg 22", null, "Cologne", null, "50667", "DE"),
        "+49 221 8901234", "info@gartenplus.de", "https://www.gartenplus.de",
        new SupplierContact("Petra", "Schulz", "Sales Manager", "p.schulz@gartenplus.de", "+49 221 8901234"),
        new SupplierBanking("DE22200400600228490100", "COBADEFFXXX", "Commerzbank"),
        Arrays.asList(
            new SupplierCertification(LegalDocumentType.ISO_9001, "DE-2023-00890", "2026-11-30", "iso9001_gartenplus.pdf", true))));
    generateDocumentsForSupplier(sup06);

    // SUP-07 — Lumber & Building Materials (DEPT-001), Switzerland
    Supplier sup07 = repo.create(supplier(
        "SUP-2020-0007", "Bauprofi AG", "AG", "CHE901234567", "CHE-123.456.789",
        "Lumber & Building Materials",
        new Address("Bahnhofstrasse 44", null, "Zurich", null, "8001", "CH"),
        "+41 44 9012345", "info@bauprofi.ch", "https://www.bauprofi.ch",
        new SupplierContact("Hans", "Müller", "Director", "h.mueller@bauprofi.ch", "+41 44 9012345"),
        new SupplierBanking("CH5604835012345678009", "CRESCHZZ80A", "Credit Suisse"),
        Arrays.asList(
            new SupplierCertification(LegalDocumentType.ISO_9001, "CH-2019-00145", "2025-06-30", "iso9001_bauprofi.pdf", true),
            new SupplierCertification(LegalDocumentType.ISO_14001, "CH-2019-00146", "2025-06-30", "iso14001_bauprofi.pdf", true))));
    generateDocumentsForSupplier(sup07);

    // SUP-08 — Safety & PPE (cross-department, Switzerland)
    Supplier sup08 = repo.create(supplier(
        "SUP-2022-0008", "SafetyFirst AG", "AG", "CHE012345678", "CHE-234.567.890",
        "Safety & Protective Equipment",
        new Address("Schutzweg 9", null, "Basel", null, "4001", "CH"),
        "+41 61 0123456", "info@safetyfirst.ch", "https://www.safetyfirst.ch",
        new SupplierContact("Rolf", "Keller", "CEO", "r.keller@safetyfirst.ch", "+41 61 0123456"),
        new SupplierBanking("CH9300762011623852957", "UBSWCHZH80A", "UBS"),
        Arrays.asList(
            new SupplierCertification(LegalDocumentType.ISO_9001, "CH-2022-00512", "2025-10-31", "iso9001_safetyfirst.pdf", true),
            new SupplierCertification(LegalDocumentType.GDPR_DPA, null, null, "gdpr_dpa_safetyfirst.pdf", true))));
    generateDocumentsForSupplier(sup08);

    // SUP-09 — Tools & Hardware (DEPT-003), precision focus
    Supplier sup09 = repo.create(supplier(
        "SUP-2021-0009", "MeasureTech GmbH", "GmbH", "DE123456780", "HRB 556677",
        "Tools & Hardware",
        new Address("Präzisionsring 3", null, "Düsseldorf", null, "40213", "DE"),
        "+49 211 1234567", "info@measuretech.de", "https://www.measuretech.de",
        new SupplierContact("Frank", "Vogel", "Sales Director", "f.vogel@measuretech.de", "+49 211 1234567"),
        new SupplierBanking("DE68210501700012345678", "SSKMDEMMXXX", "Sparkasse"),
        Arrays.asList(
            new SupplierCertification(LegalDocumentType.ISO_9001, "DE-2021-00678", "2026-07-31", "iso9001_measuretech.pdf", true),
            new SupplierCertification(LegalDocumentType.ISO_27001, "DE-2021-00679", "2026-07-31", "iso27001_measuretech.pdf", false))));
    generateDocumentsForSupplier(sup09);

    // SUP-10 — Flooring & Decor, Italy
    Supplier sup10 = repo.create(supplier(
        "SUP-2023-0010", "ItalFloor s.r.l.", "s.r.l.", "IT234567890", "MI-2345678",
        "Flooring & Decor",
        new Address("Via della Ceramica 55", null, "Milan", null, "20121", "IT"),
        "+39 02 2345678", "export@italfloor.it", "https://www.italfloor.it",
        new SupplierContact("Marco", "Rossi", "Export Director", "m.rossi@italfloor.it", "+39 02 2345678"),
        new SupplierBanking("IT60X0542811101000000123456", "ICRAITRR", "UniCredit"),
        Arrays.asList(
            new SupplierCertification(LegalDocumentType.ISO_9001, "IT-2023-00201", "2026-02-28", "iso9001_italfloor.pdf", true),
            new SupplierCertification(LegalDocumentType.ISO_14001, "IT-2023-00202", "2026-02-28", "iso14001_italfloor.pdf", false))));
    generateDocumentsForSupplier(sup10);

    Ivy.log().info("Generated 10 suppliers with legal documents.");
  }

  // ── Legal Documents ───────────────────────────────────────────────────────

  private static void generateDocumentsForSupplier(Supplier supplier) {
    LegalDocumentRepository docRepo = LegalDocumentRepository.getInstance();

    LegalDocument commercialRegister = LegalDocument.builder()
        .objectId(supplier.getSupplierId())
        .objectType(LegalDocumentObjectType.SUPPLIER)
        .documentType(LegalDocumentType.COMMERCIAL_REGISTER)
        .fileName("commercial_register_" + supplier.getSupplierId().toLowerCase() + ".pdf")
        .contentType("application/pdf")
        .fileContent(new byte[0])
        .fileSize(0)
        .description(supplier.getCommercialRegisterNo())
        .uploadedNow()
        .build();
    docRepo.save(commercialRegister);
    supplier.getRequiredDocumentIds().add(commercialRegister.getDocumentId());

    LegalDocument selfDecl = LegalDocument.builder()
        .objectId(supplier.getSupplierId())
        .objectType(LegalDocumentObjectType.SUPPLIER)
        .documentType(LegalDocumentType.SELF_DECLARATION)
        .fileName("self_declaration_" + supplier.getSupplierId().toLowerCase() + ".pdf")
        .contentType("application/pdf")
        .fileContent(new byte[0])
        .fileSize(0)
        .uploadedNow()
        .build();
    docRepo.save(selfDecl);
    supplier.getRequiredDocumentIds().add(selfDecl.getDocumentId());

    LegalDocument annualReport = LegalDocument.builder()
        .objectId(supplier.getSupplierId())
        .objectType(LegalDocumentObjectType.SUPPLIER)
        .documentType(LegalDocumentType.ANNUAL_REPORT)
        .fileName("annual_report_" + supplier.getSupplierId().toLowerCase() + ".pdf")
        .contentType("application/pdf")
        .fileContent(new byte[0])
        .fileSize(0)
        .uploadedNow()
        .build();
    docRepo.save(annualReport);
    supplier.getRequiredDocumentIds().add(annualReport.getDocumentId());

    if (supplier.getCertifications() != null) {
      for (SupplierCertification cert : supplier.getCertifications()) {
        String certFileName = cert.getDocumentReference() != null
            ? cert.getDocumentReference()
            : cert.getType().name().toLowerCase() + "_" + supplier.getSupplierId().toLowerCase() + ".pdf";
        LegalDocument certDoc = LegalDocument.builder()
            .objectId(supplier.getSupplierId())
            .objectType(LegalDocumentObjectType.SUPPLIER)
            .documentType(cert.getType())
            .fileName(certFileName)
            .contentType("application/pdf")
            .fileContent(new byte[0])
            .fileSize(0)
            .uploadedNow()
            .build();
        docRepo.save(certDoc);
        supplier.getCertificationDocumentIds().add(certDoc.getDocumentId());
      }
    }

    Ivy.repo().save(supplier);
  }

  // ── Private helpers ───────────────────────────────────────────────────────

  private static Employee employee(String username, String firstName, String lastName,
      String departmentId, String role, String email) {
    Employee emp = new Employee();
    emp.setUsername(username);
    emp.setFirstName(firstName);
    emp.setLastName(lastName);
    emp.setDepartmentId(departmentId);
    emp.setRole(role);
    emp.setEmail(email);
    return emp;
  }

  private static Department department(String id, String name, String firstLevelManager, String secondLevelManager) {
    Department dept = new Department();
    dept.setId(id);
    dept.setName(name);
    dept.setFirstLevelManager(firstLevelManager);
    dept.setSecondLevelManager(secondLevelManager);
    return dept;
  }

  private static Supplier supplier(String id, String businessName, String legalForm, String vatId,
      String commercialRegisterNo, String businessPurpose, Address address,
      String phone, String email, String website,
      SupplierContact contact, SupplierBanking banking, List<SupplierCertification> certifications) {
    Supplier s = new Supplier();
    s.setSupplierId(id);
    s.setBusinessName(businessName);
    s.setLegalForm(legalForm);
    s.setVatId(vatId);
    s.setCommercialRegisterNo(commercialRegisterNo);
    s.setBusinessPurpose(businessPurpose);
    s.setBusinessAddress(address);
    s.setPhone(phone);
    s.setEmail(email);
    s.setWebsite(website);
    s.setPrimaryContact(contact);
    s.setBanking(banking);
    s.setCertifications(certifications);
    return s;
  }

  private static SupplierPolicyRule policyRule(String target, String rule, int riskScore) {
    return new SupplierPolicyRule(target, rule, riskScore, false, RuleType.POLICY);
  }

  private static SupplierPolicyRule financialRule(String target, String rule, int riskScore, LegalDocumentType docType) {
    SupplierPolicyRule r = new SupplierPolicyRule(target, rule, riskScore, false, RuleType.FINANCIAL);
    r.setLegalDocumentType(docType);
    return r;
  }

  private static SupplierPolicyRule policyRule(String target, String rule, int riskScore, LegalDocumentType docType) {
    SupplierPolicyRule r = policyRule(target, rule, riskScore);
    if (docType.isCertification() && docType != LegalDocumentType.CERTIFICATION) {
      r.setCertificationType(docType);
    } else {
      r.setLegalDocumentType(docType);
    }
    return r;
  }
}

