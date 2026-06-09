package com.axonivy.utils.smart.workflow.demo.erp.mock;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

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

  private static final Function<MockRules, SupplierPolicyRule> TO_SUPPLIER_RULE = mockRule -> {
    SupplierPolicyRule r = new SupplierPolicyRule(
        mockRule.name(), mockRule.rule(), mockRule.riskScore(), false, mockRule.ruleType());
    LegalDocumentType docType = mockRule.docType();
    boolean isCertificationSubtype = docType != null && mockRule.ruleType() == RuleType.POLICY
        && docType.isCertification() && docType != LegalDocumentType.CERTIFICATION;
    if (isCertificationSubtype) {
      r.setCertificationType(docType);
    } else {
      r.setLegalDocumentType(docType);
    }
    return r;
  };

  public static void mock() {
    Set<RuleType> seededTypes = SupplierPolicyRuleRepository.getInstance().findAll().stream()
        .map(SupplierPolicyRule::getRuleType)
        .collect(Collectors.toSet());
    for (RuleType type : RuleType.values()) {
      if (!seededTypes.contains(type)) {
        generateRulesFor(type);
      }
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

  public static void generateRulesFor(RuleType type) {
    SupplierPolicyRuleRepository repo = SupplierPolicyRuleRepository.getInstance();
    List<SupplierPolicyRule> rules = Arrays.stream(MockRules.values())
        .filter(r -> r.ruleType() == type)
        .map(TO_SUPPLIER_RULE)
        .collect(Collectors.toList());
    rules.forEach(repo::create);
    Ivy.log().info("Generated " + rules.size() + " " + type.label() + " rules.");
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

    // SUP-02 — Tools & Hardware — *** ~60% match with TechVision GmbH ***
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

    // SUP-03 — Industrial Automation — *** ~50% match with TechVision GmbH ***
    // Same name root "TechVision", same country DE, related but different business purpose
    Supplier sup03 = repo.create(supplier(
        "SUP-2022-0003", "TechVision Systems GmbH", "GmbH", "DE456789012", "HRB 223344",
        "Industrial Automation & Machinery",
        new Address("Werkzeugstraße 33", null, "Stuttgart", null, "70173", "DE"),
        "+49 711 5678901", "contact@techvision-systems.de", "https://www.techvision-systems.de",
        new SupplierContact("Anna", "Weber", "Sales Director", "a.weber@techvision-systems.de", "+49 711 5678901"),
        new SupplierBanking("DE21200400300002345678", "COBADEFFXXX", "HypoVereinsbank"),
        Arrays.asList(
            new SupplierCertification(LegalDocumentType.ISO_9001, "DE-2022-00567", "2025-12-31", "iso9001_techvision_systems.pdf", true))));
    generateDocumentsForSupplier(sup03);

    // SUP-03b — Software & IT — *** ~40% match with TechVision GmbH ***
    // Same name root "TechVision", same country DE, clearly different business purpose
    Supplier sup03b = repo.create(supplier(
        "SUP-2023-0003B", "TechVision International GmbH", "GmbH", "DE567890120", "HRB 334422",
        "Software & IT Infrastructure",
        new Address("Digitalstraße 9", null, "Berlin", null, "10179", "DE"),
        "+49 30 1234567", "info@techvision-intl.de", "https://www.techvision-intl.de",
        new SupplierContact("Lars", "Hoffmann", "CEO", "l.hoffmann@techvision-intl.de", "+49 30 1234567"),
        new SupplierBanking("DE75512108001245126200", "BELADEBEXXX", "Deutsche Bank"),
        Arrays.asList(
            new SupplierCertification(LegalDocumentType.ISO_27001, "DE-2023-00123", "2026-06-30", "iso27001_techvision_intl.pdf", true))));
    generateDocumentsForSupplier(sup03b);

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

}