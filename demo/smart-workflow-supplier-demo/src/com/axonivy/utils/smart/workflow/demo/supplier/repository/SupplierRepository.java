package com.axonivy.utils.smart.workflow.demo.supplier.repository;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.smart.workflow.demo.supplier.Supplier;
import com.axonivy.utils.smart.workflow.demo.supplier.agent.SupplierAgentResponse;
import com.axonivy.utils.smart.workflow.utils.IdGenerationUtils;

import ch.ivyteam.ivy.business.data.store.search.Query;
import ch.ivyteam.ivy.environment.Ivy;

public class SupplierRepository {

  private static final String FIELD_SUPPLIER_ID = "supplierId";
  private static final String FIELD_BUSINESS_NAME = "businessName";

  private static final Set<String> LEGAL_FORM_STOPWORDS = Set.of(
      "gmbh", "ag", "ltd", "llc", "inc", "corp", "plc", "bv", "nv", "sa", "srl",
      "s.r.l.", "s.a.", "ug", "kg", "ohg", "gbr", "ek", "e.k.", "co", "&", "and",
      "partner", "gruppe", "group", "holding", "systems", "solutions"
  );

  private static SupplierRepository instance;

  public static SupplierRepository getInstance() {
    if (instance == null) {
      instance = new SupplierRepository();
    }
    return instance;
  }

  public Supplier create(Supplier supplier) {
    if (supplier == null) {
      throw new IllegalArgumentException("Supplier cannot be null");
    }

    if (StringUtils.isBlank(supplier.getSupplierId())) {
      supplier.setSupplierId(IdGenerationUtils.generateRandomId());
    }

    Ivy.repo().save(supplier);
    return supplier;
  }

  public List<Supplier> findAll() {
    return Ivy.repo().search(Supplier.class).execute().getAll();
  }

  public Optional<Supplier> update(Supplier supplier) {
    if (supplier == null) {
      return Optional.empty();
    }

    Supplier existing = findById(supplier.getSupplierId()).orElse(null);
    if (existing == null) {
      return Optional.empty();
    }

    try {
      existing.setBusinessName(supplier.getBusinessName());
      existing.setLegalForm(supplier.getLegalForm());
      existing.setVatId(supplier.getVatId());
      existing.setCommercialRegisterNo(supplier.getCommercialRegisterNo());
      existing.setBusinessPurpose(supplier.getBusinessPurpose());
      existing.setBusinessAddress(supplier.getBusinessAddress());
      existing.setPhone(supplier.getPhone());
      existing.setEmail(supplier.getEmail());
      existing.setWebsite(supplier.getWebsite());
      existing.setPrimaryContact(supplier.getPrimaryContact());
      existing.setBanking(supplier.getBanking());
      existing.setCertifications(supplier.getCertifications());

      Ivy.repo().save(existing);
    } catch (Exception e) {
      Ivy.log().error(e);
    }
    return findById(supplier.getSupplierId());
  }

  public void delete(Supplier supplier) {
    if (supplier == null) {
      return;
    }
    findById(supplier.getSupplierId()).ifPresent(Ivy.repo()::delete);
  }

  public Optional<Supplier> findById(String id) {
    return Optional.ofNullable(
        Ivy.repo().search(Supplier.class).textField(FIELD_SUPPLIER_ID).isEqualToIgnoringCase(id).execute()
            .getFirst());
  }

  public List<Supplier> findByCriteria(SupplierSearchCriteria criteria) {
    if (criteria == null || !hasAnyFilter(criteria)) {
      return Ivy.repo().search(Supplier.class).execute().getAll();
    }
    return buildFilteredSearch(criteria).execute().getAll();
  }

  public List<Supplier> findMatchingsByCriteria(SupplierSearchCriteria criteria) {
    if (criteria == null || !hasAnyFilter(criteria)) {
      return Ivy.repo().search(Supplier.class).execute().getAll();
    }

    var search = buildFilteredSearch(criteria);

    if (StringUtils.isNotBlank(criteria.getBusinessNameContains())) {
      for (String token : criteria.getBusinessNameContains().split("\\s+")) {
        if (StringUtils.isNotBlank(token)
            && !LEGAL_FORM_STOPWORDS.contains(token.toLowerCase(Locale.ROOT))) {
          search.score().textField(FIELD_BUSINESS_NAME).query(token).limit(100);
        }
      }
    }

    return search.execute().getAll();
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

  private Query<Supplier> buildFilteredSearch(SupplierSearchCriteria criteria) {
    var search = Ivy.repo().search(Supplier.class);

    if (StringUtils.isNotBlank(criteria.getBusinessNameContains())) {
      String cleanedName = Arrays.stream(criteria.getBusinessNameContains().split("\\s+"))
          .filter(t -> StringUtils.isNotBlank(t) && !LEGAL_FORM_STOPWORDS.contains(t.toLowerCase(Locale.ROOT)))
          .collect(Collectors.joining(" "));
      if (StringUtils.isNotBlank(cleanedName)) {
        search.filter(search.textField(FIELD_BUSINESS_NAME).containsAllWordPatterns(cleanedName));
      }
    }

    return search;
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

    var search = Ivy.repo().search(Supplier.class);

    if (StringUtils.isNotBlank(criteria.getBusinessNameContains())) {
      String cleanedName = Arrays.stream(criteria.getBusinessNameContains().split("\\s+"))
          .filter(t -> StringUtils.isNotBlank(t) && !LEGAL_FORM_STOPWORDS.contains(t.toLowerCase(Locale.ROOT)))
          .collect(Collectors.joining(" "));
      if (StringUtils.isNotBlank(cleanedName)) {
        search.filter(search.textField(FIELD_BUSINESS_NAME).containsAllWordPatterns(cleanedName)).or();
      }
    }

    return Optional.ofNullable(search.execute().getFirst());
  }
}