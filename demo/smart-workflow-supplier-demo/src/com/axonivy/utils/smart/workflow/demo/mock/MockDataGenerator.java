package com.axonivy.utils.smart.workflow.demo.mock;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.axonivy.utils.smart.workflow.demo.common.Address;
import com.axonivy.utils.smart.workflow.demo.department.Department;
import com.axonivy.utils.smart.workflow.demo.department.DepartmentRepository;
import com.axonivy.utils.smart.workflow.demo.document.LegalDocument;
import com.axonivy.utils.smart.workflow.demo.document.LegalDocumentBuilder;
import com.axonivy.utils.smart.workflow.demo.document.enums.LegalDocumentObjectType;
import com.axonivy.utils.smart.workflow.demo.document.enums.LegalDocumentType;
import com.axonivy.utils.smart.workflow.demo.document.repository.LegalDocumentRepository;
import com.axonivy.utils.smart.workflow.demo.employee.Employee;
import com.axonivy.utils.smart.workflow.demo.employee.EmployeeRepository;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.enums.RuleType;
import com.axonivy.utils.smart.workflow.demo.supplier.Supplier;
import com.axonivy.utils.smart.workflow.demo.supplier.SupplierBanking;
import com.axonivy.utils.smart.workflow.demo.supplier.SupplierCertification;
import com.axonivy.utils.smart.workflow.demo.supplier.SupplierContact;
import com.axonivy.utils.smart.workflow.demo.supplier.SupplierPolicyRule;
import com.axonivy.utils.smart.workflow.demo.supplier.repository.SupplierPolicyRuleRepository;
import com.axonivy.utils.smart.workflow.demo.supplier.repository.SupplierRepository;
import com.axonivy.utils.smart.workflow.demo.utils.VectorStoreRestClient;
import com.axonivy.utils.smart.workflow.rag.pipeline.internal.OpenSearchIngestor;

import ch.ivyteam.ivy.environment.Ivy;

public class MockDataGenerator {

  private static final Function<MockRules, SupplierPolicyRule> TO_SUPPLIER_RULE = mockRule -> {
    SupplierPolicyRule r = new SupplierPolicyRule();
    r.setTarget(mockRule.name());
    r.setRule(mockRule.rule());
    r.setRiskScore(mockRule.riskScore());
    r.setPassed(false);
    r.setRuleType(mockRule.ruleType());
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

    Supplier sup01 = repo.create(supplier(
        "SUP-2019-0001", "Holzmann & Partner GmbH", "GmbH", "DE198765432", "HRB 67543",
        "Lumber & Building Materials",
        address("Holzweg 12", null, "Hamburg", null, "20095", "DE"),
        "+49 40 9876543", "info@holzmann-partner.de", "https://www.holzmann-partner.de",
        contact("Klaus", "Holzmann", "CEO", "k.holzmann@holzmann-partner.de", "+49 40 9876543"),
        banking("DE12500105170648489890", "BELADEBEXXX", "Deutsche Bank"),
        Arrays.asList(
            certification(LegalDocumentType.ISO_9001,  "DE-2021-00781", "2026-05-31", "iso9001_holzmann.pdf",  true),
            certification(LegalDocumentType.ISO_14001, "DE-2021-00782", "2026-05-31", "iso14001_holzmann.pdf", true))));
    generateDocumentsForSupplier(sup01);

    Supplier sup02 = repo.create(supplier(
        "SUP-2021-0002", "TechVision AG", "AG", "DE345678901", "HRB 112233",
        "Tools & Hardware",
        address("Industriestraße 7", null, "Munich", null, "80339", "DE"),
        "+49 89 3456789", "info@techvision-ag.de", "https://www.techvision-ag.de",
        contact("Michael", "Bauer", "Managing Director", "m.bauer@techvision-ag.de", "+49 89 3456789"),
        banking("DE89370400440532013000", "COBADEFFXXX", "Commerzbank"),
        Arrays.asList(
            certification(LegalDocumentType.ISO_9001, "DE-2020-00412", "2026-03-15", "iso9001_techvision_ag.pdf", true))));
    generateDocumentsForSupplier(sup02);

    Supplier sup03 = repo.create(supplier(
        "SUP-2022-0003", "TechVision Systems GmbH", "GmbH", "DE456789012", "HRB 223344",
        "Industrial Automation & Machinery",
        address("Werkzeugstraße 33", null, "Stuttgart", null, "70173", "DE"),
        "+49 711 5678901", "contact@techvision-systems.de", "https://www.techvision-systems.de",
        contact("Anna", "Weber", "Sales Director", "a.weber@techvision-systems.de", "+49 711 5678901"),
        banking("DE21200400300002345678", "COBADEFFXXX", "HypoVereinsbank"),
        Arrays.asList(
            certification(LegalDocumentType.ISO_9001, "DE-2022-00567", "2025-12-31", "iso9001_techvision_systems.pdf", true))));
    generateDocumentsForSupplier(sup03);

    Supplier sup03b = repo.create(supplier(
        "SUP-2023-0003B", "TechVision International GmbH", "GmbH", "DE567890120", "HRB 334422",
        "Software & IT Infrastructure",
        address("Digitalstraße 9", null, "Berlin", null, "10179", "DE"),
        "+49 30 1234567", "info@techvision-intl.de", "https://www.techvision-intl.de",
        contact("Lars", "Hoffmann", "CEO", "l.hoffmann@techvision-intl.de", "+49 30 1234567"),
        banking("DE75512108001245126200", "BELADEBEXXX", "Deutsche Bank"),
        Arrays.asList(
            certification(LegalDocumentType.ISO_27001, "DE-2023-00123", "2026-06-30", "iso27001_techvision_intl.pdf", true))));
    generateDocumentsForSupplier(sup03b);

    Supplier sup04 = repo.create(supplier(
        "SUP-2020-0004", "NordElektro GmbH", "GmbH", "DE567890123", "HRB 334455",
        "Electrical & Plumbing",
        address("Stromweg 5", null, "Berlin", null, "10115", "DE"),
        "+49 30 6789012", "info@nordelektro.de", "https://www.nordelektro.de",
        contact("Stefan", "Klein", "CEO", "s.klein@nordelektro.de", "+49 30 6789012"),
        banking("DE75512108001245126199", "SSKMDEMM", "Stadtsparkasse"),
        Arrays.asList(
            certification(LegalDocumentType.ISO_9001,  "DE-2020-00333", "2025-08-31", "iso9001_nordelektro.pdf",  true),
            certification(LegalDocumentType.ISO_27001, "DE-2020-00334", "2025-08-31", "iso27001_nordelektro.pdf", true))));
    generateDocumentsForSupplier(sup04);

    Supplier sup05 = repo.create(supplier(
        "SUP-2021-0005", "AlpenFloor AG", "AG", "AT678901234", "FN 123456a",
        "Flooring & Decor",
        address("Parkettallee 18", null, "Vienna", null, "1010", "AT"),
        "+43 1 7890123", "office@alpenfloor.at", "https://www.alpenfloor.at",
        contact("Eva", "Gruber", "Export Manager", "e.gruber@alpenfloor.at", "+43 1 7890123"),
        banking("AT483200000012345864", "RLNWATWWGD0", "Raiffeisenbank"),
        Arrays.asList(
            certification(LegalDocumentType.ISO_14001, "AT-2021-00210", "2026-09-30", "iso14001_alpenfloor.pdf", true))));
    generateDocumentsForSupplier(sup05);

    Supplier sup06 = repo.create(supplier(
        "SUP-2023-0006", "GartenPlus GmbH", "GmbH", "DE789012345", "HRB 445566",
        "Garden & Outdoor Living",
        address("Gartenweg 22", null, "Cologne", null, "50667", "DE"),
        "+49 221 8901234", "info@gartenplus.de", "https://www.gartenplus.de",
        contact("Petra", "Schulz", "Sales Manager", "p.schulz@gartenplus.de", "+49 221 8901234"),
        banking("DE22200400600228490100", "COBADEFFXXX", "Commerzbank"),
        Arrays.asList(
            certification(LegalDocumentType.ISO_9001, "DE-2023-00890", "2026-11-30", "iso9001_gartenplus.pdf", true))));
    generateDocumentsForSupplier(sup06);

    Supplier sup07 = repo.create(supplier(
        "SUP-2020-0007", "Bauprofi AG", "AG", "CHE901234567", "CHE-123.456.789",
        "Lumber & Building Materials",
        address("Bahnhofstrasse 44", null, "Zurich", null, "8001", "CH"),
        "+41 44 9012345", "info@bauprofi.ch", "https://www.bauprofi.ch",
        contact("Hans", "Müller", "Director", "h.mueller@bauprofi.ch", "+41 44 9012345"),
        banking("CH5604835012345678009", "CRESCHZZ80A", "Credit Suisse"),
        Arrays.asList(
            certification(LegalDocumentType.ISO_9001,  "CH-2019-00145", "2025-06-30", "iso9001_bauprofi.pdf",  true),
            certification(LegalDocumentType.ISO_14001, "CH-2019-00146", "2025-06-30", "iso14001_bauprofi.pdf", true))));
    generateDocumentsForSupplier(sup07);

    Supplier sup08 = repo.create(supplier(
        "SUP-2022-0008", "SafetyFirst AG", "AG", "CHE012345678", "CHE-234.567.890",
        "Safety & Protective Equipment",
        address("Schutzweg 9", null, "Basel", null, "4001", "CH"),
        "+41 61 0123456", "info@safetyfirst.ch", "https://www.safetyfirst.ch",
        contact("Rolf", "Keller", "CEO", "r.keller@safetyfirst.ch", "+41 61 0123456"),
        banking("CH9300762011623852957", "UBSWCHZH80A", "UBS"),
        Arrays.asList(
            certification(LegalDocumentType.ISO_9001, "CH-2022-00512", "2025-10-31", "iso9001_safetyfirst.pdf", true),
            certification(LegalDocumentType.GDPR_DPA, null,            null,         "gdpr_dpa_safetyfirst.pdf", true))));
    generateDocumentsForSupplier(sup08);

    Supplier sup09 = repo.create(supplier(
        "SUP-2021-0009", "MeasureTech GmbH", "GmbH", "DE123456780", "HRB 556677",
        "Tools & Hardware",
        address("Präzisionsring 3", null, "Düsseldorf", null, "40213", "DE"),
        "+49 211 1234567", "info@measuretech.de", "https://www.measuretech.de",
        contact("Frank", "Vogel", "Sales Director", "f.vogel@measuretech.de", "+49 211 1234567"),
        banking("DE68210501700012345678", "SSKMDEMMXXX", "Sparkasse"),
        Arrays.asList(
            certification(LegalDocumentType.ISO_9001,  "DE-2021-00678", "2026-07-31", "iso9001_measuretech.pdf",  true),
            certification(LegalDocumentType.ISO_27001, "DE-2021-00679", "2026-07-31", "iso27001_measuretech.pdf", false))));
    generateDocumentsForSupplier(sup09);

    Supplier sup10 = repo.create(supplier(
        "SUP-2023-0010", "ItalFloor s.r.l.", "s.r.l.", "IT234567890", "MI-2345678",
        "Flooring & Decor",
        address("Via della Ceramica 55", null, "Milan", null, "20121", "IT"),
        "+39 02 2345678", "export@italfloor.it", "https://www.italfloor.it",
        contact("Marco", "Rossi", "Export Director", "m.rossi@italfloor.it", "+39 02 2345678"),
        banking("IT60X0542811101000000123456", "ICRAITRR", "UniCredit"),
        Arrays.asList(
            certification(LegalDocumentType.ISO_9001,  "IT-2023-00201", "2026-02-28", "iso9001_italfloor.pdf",  true),
            certification(LegalDocumentType.ISO_14001, "IT-2023-00202", "2026-02-28", "iso14001_italfloor.pdf", false))));
    generateDocumentsForSupplier(sup10);

    Ivy.log().info("Generated 10 suppliers with legal documents.");
  }

  // ── Legal Documents ───────────────────────────────────────────────────────

  private static void generateDocumentsForSupplier(Supplier supplier) {
    LegalDocumentRepository docRepo = LegalDocumentRepository.getInstance();

    LegalDocument commercialRegister = new LegalDocumentBuilder()
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

    LegalDocument selfDecl = new LegalDocumentBuilder()
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

    LegalDocument annualReport = new LegalDocumentBuilder()
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
        LegalDocument certDoc = new LegalDocumentBuilder()
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

  private static Address address(String street1, String street2, String city, String state,
      String zipCode, String country) {
    Address a = new Address();
    a.setStreet1(street1);
    a.setStreet2(street2);
    a.setCity(city);
    a.setState(state);
    a.setZipCode(zipCode);
    a.setCountry(country);
    return a;
  }

  private static SupplierContact contact(String firstName, String lastName, String jobTitle,
      String email, String phone) {
    SupplierContact c = new SupplierContact();
    c.setFirstName(firstName);
    c.setLastName(lastName);
    c.setJobTitle(jobTitle);
    c.setEmail(email);
    c.setPhone(phone);
    return c;
  }

  private static SupplierBanking banking(String iban, String bic, String bankName) {
    SupplierBanking b = new SupplierBanking();
    b.setIban(iban);
    b.setBic(bic);
    b.setBankName(bankName);
    return b;
  }

  private static SupplierCertification certification(LegalDocumentType type, String certNumber,
      String expiryDate, String documentReference, boolean uploaded) {
    SupplierCertification c = new SupplierCertification();
    c.setType(type);
    c.setCertNumber(certNumber);
    c.setExpiryDate(expiryDate);
    c.setDocumentReference(documentReference);
    c.setUploaded(uploaded);
    return c;
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
