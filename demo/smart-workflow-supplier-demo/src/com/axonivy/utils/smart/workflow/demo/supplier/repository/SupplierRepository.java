package com.axonivy.utils.smart.workflow.demo.supplier.repository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.smart.workflow.demo.AbstractMockRepository;
import com.axonivy.utils.smart.workflow.demo.common.Address;
import com.axonivy.utils.smart.workflow.demo.document.enums.LegalDocumentType;
import com.axonivy.utils.smart.workflow.demo.supplier.Supplier;
import com.axonivy.utils.smart.workflow.demo.supplier.SupplierBanking;
import com.axonivy.utils.smart.workflow.demo.supplier.SupplierCertification;
import com.axonivy.utils.smart.workflow.demo.supplier.SupplierContact;
import com.axonivy.utils.smart.workflow.demo.supplier.agent.SupplierAgentResponse;
import com.axonivy.utils.smart.workflow.utils.IdGenerationUtils;
import com.fasterxml.jackson.core.type.TypeReference;

import ch.ivyteam.ivy.environment.Ivy;

public class SupplierRepository extends AbstractMockRepository<Supplier> {

  private static final String FIELD = "MOCK_SUPPLIERS";
  private static final TypeReference<List<Supplier>> LIST_TYPE = new TypeReference<List<Supplier>>() {};

  private static SupplierRepository instance;

  public static SupplierRepository getInstance() {
    if (instance == null) {
      instance = new SupplierRepository();
    }
    return instance;
  }

  @Override
  protected String getField() {
    return FIELD;
  }

  @Override
  protected TypeReference<List<Supplier>> getListType() {
    return LIST_TYPE;
  }

  @Override
  protected List<Supplier> createMockData() {
    List<Supplier> list = new ArrayList<>();

    list.add(supplier("SUP-2019-0001", "Holzmann & Partner GmbH", "GmbH", "DE198765432", "HRB 67543",
        "Lumber & Building Materials",
        address("Holzweg 12", "Hamburg", "20095", "DE"),
        "+49 40 9876543", "info@holzmann-partner.de", "https://www.holzmann-partner.de",
        contact("Klaus", "Holzmann", "CEO", "k.holzmann@holzmann-partner.de", "+49 40 9876543"),
        banking("DE12500105170648489890", "BELADEBEXXX", "Deutsche Bank"),
        Arrays.asList(
            certification(LegalDocumentType.ISO_9001,  "DE-2021-00781", "2026-05-31", "iso9001_holzmann.pdf",  true),
            certification(LegalDocumentType.ISO_14001, "DE-2021-00782", "2026-05-31", "iso14001_holzmann.pdf", true))));

    list.add(supplier("SUP-2021-0002", "TechVision AG", "AG", "DE345678901", "HRB 112233",
        "Tools & Hardware",
        address("Industriestraße 7", "Munich", "80339", "DE"),
        "+49 89 3456789", "info@techvision-ag.de", "https://www.techvision-ag.de",
        contact("Michael", "Bauer", "Managing Director", "m.bauer@techvision-ag.de", "+49 89 3456789"),
        banking("DE89370400440532013000", "COBADEFFXXX", "Commerzbank"),
        Arrays.asList(
            certification(LegalDocumentType.ISO_9001, "DE-2020-00412", "2026-03-15", "iso9001_techvision_ag.pdf", true))));

    list.add(supplier("SUP-2022-0003", "TechVision Systems GmbH", "GmbH", "DE456789012", "HRB 223344",
        "Industrial Automation & Machinery",
        address("Werkzeugstraße 33", "Stuttgart", "70173", "DE"),
        "+49 711 5678901", "contact@techvision-systems.de", "https://www.techvision-systems.de",
        contact("Anna", "Weber", "Sales Director", "a.weber@techvision-systems.de", "+49 711 5678901"),
        banking("DE21200400300002345678", "COBADEFFXXX", "HypoVereinsbank"),
        Arrays.asList(
            certification(LegalDocumentType.ISO_9001, "DE-2022-00567", "2025-12-31", "iso9001_techvision_systems.pdf", true))));

    list.add(supplier("SUP-2023-0003B", "TechVision International GmbH", "GmbH", "DE567890120", "HRB 334422",
        "Software & IT Infrastructure",
        address("Digitalstraße 9", "Berlin", "10179", "DE"),
        "+49 30 1234567", "info@techvision-intl.de", "https://www.techvision-intl.de",
        contact("Lars", "Hoffmann", "CEO", "l.hoffmann@techvision-intl.de", "+49 30 1234567"),
        banking("DE75512108001245126200", "BELADEBEXXX", "Deutsche Bank"),
        Arrays.asList(
            certification(LegalDocumentType.ISO_27001, "DE-2023-00123", "2026-06-30", "iso27001_techvision_intl.pdf", true))));

    list.add(supplier("SUP-2020-0004", "NordElektro GmbH", "GmbH", "DE567890123", "HRB 334455",
        "Electrical & Plumbing",
        address("Stromweg 5", "Berlin", "10115", "DE"),
        "+49 30 6789012", "info@nordelektro.de", "https://www.nordelektro.de",
        contact("Stefan", "Klein", "CEO", "s.klein@nordelektro.de", "+49 30 6789012"),
        banking("DE75512108001245126199", "SSKMDEMM", "Stadtsparkasse"),
        Arrays.asList(
            certification(LegalDocumentType.ISO_9001,  "DE-2020-00333", "2025-08-31", "iso9001_nordelektro.pdf",  true),
            certification(LegalDocumentType.ISO_27001, "DE-2020-00334", "2025-08-31", "iso27001_nordelektro.pdf", true))));

    list.add(supplier("SUP-2021-0005", "AlpenFloor AG", "AG", "AT678901234", "FN 123456a",
        "Flooring & Decor",
        address("Parkettallee 18", "Vienna", "1010", "AT"),
        "+43 1 7890123", "office@alpenfloor.at", "https://www.alpenfloor.at",
        contact("Eva", "Gruber", "Export Manager", "e.gruber@alpenfloor.at", "+43 1 7890123"),
        banking("AT483200000012345864", "RLNWATWWGD0", "Raiffeisenbank"),
        Arrays.asList(
            certification(LegalDocumentType.ISO_14001, "AT-2021-00210", "2026-09-30", "iso14001_alpenfloor.pdf", true))));

    list.add(supplier("SUP-2023-0006", "GartenPlus GmbH", "GmbH", "DE789012345", "HRB 445566",
        "Garden & Outdoor Living",
        address("Gartenweg 22", "Cologne", "50667", "DE"),
        "+49 221 8901234", "info@gartenplus.de", "https://www.gartenplus.de",
        contact("Petra", "Schulz", "Sales Manager", "p.schulz@gartenplus.de", "+49 221 8901234"),
        banking("DE22200400600228490100", "COBADEFFXXX", "Commerzbank"),
        Arrays.asList(
            certification(LegalDocumentType.ISO_9001, "DE-2023-00890", "2026-11-30", "iso9001_gartenplus.pdf", true))));

    list.add(supplier("SUP-2020-0007", "Bauprofi AG", "AG", "CHE901234567", "CHE-123.456.789",
        "Lumber & Building Materials",
        address("Bahnhofstrasse 44", "Zurich", "8001", "CH"),
        "+41 44 9012345", "info@bauprofi.ch", "https://www.bauprofi.ch",
        contact("Hans", "Müller", "Director", "h.mueller@bauprofi.ch", "+41 44 9012345"),
        banking("CH5604835012345678009", "CRESCHZZ80A", "Credit Suisse"),
        Arrays.asList(
            certification(LegalDocumentType.ISO_9001,  "CH-2019-00145", "2025-06-30", "iso9001_bauprofi.pdf",  true),
            certification(LegalDocumentType.ISO_14001, "CH-2019-00146", "2025-06-30", "iso14001_bauprofi.pdf", true))));

    list.add(supplier("SUP-2022-0008", "SafetyFirst AG", "AG", "CHE012345678", "CHE-234.567.890",
        "Safety & Protective Equipment",
        address("Schutzweg 9", "Basel", "4001", "CH"),
        "+41 61 0123456", "info@safetyfirst.ch", "https://www.safetyfirst.ch",
        contact("Rolf", "Keller", "CEO", "r.keller@safetyfirst.ch", "+41 61 0123456"),
        banking("CH9300762011623852957", "UBSWCHZH80A", "UBS"),
        Arrays.asList(
            certification(LegalDocumentType.ISO_9001, "CH-2022-00512", "2025-10-31", "iso9001_safetyfirst.pdf", true),
            certification(LegalDocumentType.GDPR_DPA, null,            null,         "gdpr_dpa_safetyfirst.pdf", true))));

    list.add(supplier("SUP-2021-0009", "MeasureTech GmbH", "GmbH", "DE123456780", "HRB 556677",
        "Tools & Hardware",
        address("Präzisionsring 3", "Düsseldorf", "40213", "DE"),
        "+49 211 1234567", "info@measuretech.de", "https://www.measuretech.de",
        contact("Frank", "Vogel", "Sales Director", "f.vogel@measuretech.de", "+49 211 1234567"),
        banking("DE68210501700012345678", "SSKMDEMMXXX", "Sparkasse"),
        Arrays.asList(
            certification(LegalDocumentType.ISO_9001,  "DE-2021-00678", "2026-07-31", "iso9001_measuretech.pdf",  true),
            certification(LegalDocumentType.ISO_27001, "DE-2021-00679", "2026-07-31", "iso27001_measuretech.pdf", false))));

    list.add(supplier("SUP-2023-0010", "ItalFloor s.r.l.", "s.r.l.", "IT234567890", "MI-2345678",
        "Flooring & Decor",
        address("Via della Ceramica 55", "Milan", "20121", "IT"),
        "+39 02 2345678", "export@italfloor.it", "https://www.italfloor.it",
        contact("Marco", "Rossi", "Export Director", "m.rossi@italfloor.it", "+39 02 2345678"),
        banking("IT60X0542811101000000123456", "ICRAITRR", "UniCredit"),
        Arrays.asList(
            certification(LegalDocumentType.ISO_9001,  "IT-2023-00201", "2026-02-28", "iso9001_italfloor.pdf",  true),
            certification(LegalDocumentType.ISO_14001, "IT-2023-00202", "2026-02-28", "iso14001_italfloor.pdf", false))));

    return list;
  }

  public Supplier create(Supplier supplier) {
    if (supplier == null) {
      throw new IllegalArgumentException("Supplier cannot be null");
    }
    if (StringUtils.isBlank(supplier.getSupplierId())) {
      supplier.setSupplierId(IdGenerationUtils.generateRandomId());
    }
    List<Supplier> list = new ArrayList<>(findAll());
    list.add(supplier);
    save(list);
    return supplier;
  }

  public Optional<Supplier> update(Supplier supplier) {
    if (supplier == null) {
      return Optional.empty();
    }
    List<Supplier> list = new ArrayList<>(findAll());
    int idx = indexById(list, supplier.getSupplierId());
    if (idx < 0) {
      return Optional.empty();
    }
    list.set(idx, supplier);
    try {
      save(list);
    } catch (Exception e) {
      Ivy.log().error(e);
    }
    return Optional.of(supplier);
  }

  public void delete(Supplier supplier) {
    if (supplier == null) {
      return;
    }
    List<Supplier> list = new ArrayList<>(findAll());
    list.removeIf(s -> supplier.getSupplierId().equals(s.getSupplierId()));
    save(list);
  }

  public Optional<Supplier> findById(String id) {
    return findAll().stream()
        .filter(s -> id.equalsIgnoreCase(s.getSupplierId()))
        .findFirst();
  }

  public List<Supplier> findByCriteria(SupplierSearchCriteria criteria) {
    List<Supplier> all = findAll();
    if (criteria == null || !hasAnyFilter(criteria)) {
      return all;
    }
    return applyFilters(all.stream(), criteria).collect(Collectors.toList());
  }

  public List<Supplier> findMatchingsByCriteria(SupplierSearchCriteria criteria) {
    List<Supplier> all = findAll();
    if (criteria == null || !hasAnyFilter(criteria)) {
      return all;
    }
    return applyFilters(all.stream(), criteria).collect(Collectors.toList());
  }

  public SupplierAgentResponse findSimilarSuppliers(SupplierSearchCriteria criteria) {
    List<Supplier> matches = findMatchingsByCriteria(criteria);
    int size = matches != null ? matches.size() : 0;

    SupplierAgentResponse response = new SupplierAgentResponse();
    response.setSuppliers(matches);
    response.setIsSupplierExisting(size > 0);
    response.setMatchScore(size > 0 ? 85 : 0);
    response.setFeedback(size > 0
        ? "Found " + size + " similar supplier(s) in the database."
        : "No similar suppliers found in the database.");

    return response;
  }

  public Optional<Supplier> findExactSupplier(SupplierSearchCriteria criteria) {
    if (criteria == null || !hasAnyFilter(criteria)) {
      return Optional.empty();
    }
    return applyFilters(findAll().stream(), criteria).findFirst();
  }

  private Stream<Supplier> applyFilters(Stream<Supplier> stream, SupplierSearchCriteria criteria) {
    if (StringUtils.isNotBlank(criteria.getSupplierId())) {
      stream = stream.filter(s -> criteria.getSupplierId().equalsIgnoreCase(s.getSupplierId()));
    }
    if (StringUtils.isNotBlank(criteria.getBusinessNameContains())) {
      String lower = criteria.getBusinessNameContains().toLowerCase(Locale.ROOT);
      stream = stream.filter(s -> s.getBusinessName() != null
          && s.getBusinessName().toLowerCase(Locale.ROOT).contains(lower));
    }
    if (StringUtils.isNotBlank(criteria.getVatId())) {
      stream = stream.filter(s -> criteria.getVatId().equalsIgnoreCase(s.getVatId()));
    }
    if (StringUtils.isNotBlank(criteria.getEmail())) {
      stream = stream.filter(s -> criteria.getEmail().equalsIgnoreCase(s.getEmail()));
    }
    if (StringUtils.isNotBlank(criteria.getPhone())) {
      stream = stream.filter(s -> criteria.getPhone().equals(s.getPhone()));
    }
    if (StringUtils.isNotBlank(criteria.getWebsite())) {
      stream = stream.filter(s -> criteria.getWebsite().equalsIgnoreCase(s.getWebsite()));
    }
    if (StringUtils.isNotBlank(criteria.getBusinessPurposeContains())) {
      String lower = criteria.getBusinessPurposeContains().toLowerCase(Locale.ROOT);
      stream = stream.filter(s -> s.getBusinessPurpose() != null
          && s.getBusinessPurpose().toLowerCase(Locale.ROOT).contains(lower));
    }
    if (StringUtils.isNotBlank(criteria.getCountry())) {
      stream = stream.filter(s -> s.getBusinessAddress() != null
          && criteria.getCountry().equalsIgnoreCase(s.getBusinessAddress().getCountry()));
    }
    return stream;
  }

  private static boolean hasAnyFilter(SupplierSearchCriteria c) {
    return StringUtils.isNotBlank(c.getSupplierId())
        || StringUtils.isNotBlank(c.getBusinessNameContains())
        || c.getBusinessAddress() != null
        || StringUtils.isNotBlank(c.getPhone())
        || StringUtils.isNotBlank(c.getEmail())
        || StringUtils.isNotBlank(c.getWebsite())
        || StringUtils.isNotBlank(c.getVatId())
        || StringUtils.isNotBlank(c.getBusinessPurposeContains())
        || StringUtils.isNotBlank(c.getCountry());
  }

  private int indexById(List<Supplier> list, String supplierId) {
    for (int i = 0; i < list.size(); i++) {
      if (supplierId.equals(list.get(i).getSupplierId())) {
        return i;
      }
    }
    return -1;
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

  private static Address address(String street, String city, String zipCode, String country) {
    Address a = new Address();
    a.setStreet1(street);
    a.setCity(city);
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
}
