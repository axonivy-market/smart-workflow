package com.axonivy.utils.smart.orchestrator.demo.shopping.supplier;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.smart.orchestrator.utils.IdGenerationUtils;

import ch.ivyteam.ivy.business.data.store.search.Filter;
import ch.ivyteam.ivy.environment.Ivy;

public class SupplierRepository {

  private static final String FIELD_SUPPLIER_ID = "supplierId";
  private static final String FIELD_BUSINESS_NAME = "businessName";
  private static final String FIELD_PHONE = "phone";
  private static final String FIELD_EMAIL = "email";
  private static final String FIELD_WEBSITE = "website";
  private static final String FIELD_BUSINESS_ADDRESS = "businessAddress";

  private static SupplierRepository instance;

  public static SupplierRepository getInstance() {
    if (instance == null) {
      instance = new SupplierRepository();
    }
    return instance;
  }

  /**
   * Creates or saves a new supplier.
   *
   * @param supplier the supplier to save
   * @return the persisted supplier
   * @throws IllegalArgumentException if supplier is null
   */
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

  /**
   * Retrieves all suppliers.
   *
   * @return list of all suppliers
   */
  public List<Supplier> findAll() {
    return Ivy.repo().search(Supplier.class).execute().getAll();
  }

  /**
   * Updates an existing supplier.
   *
   * @param supplier the supplier to update
   * @return the updated supplier
   */
  public Supplier update(Supplier supplier) {
    if (supplier == null) {
      return null;
    }

    Supplier existing = findById(supplier.getSupplierId());

    try {
      existing.setBusinessName(supplier.getBusinessName());
      existing.setBusinessAddress(supplier.getBusinessAddress());
      existing.setPhone(supplier.getPhone());
      existing.setEmail(supplier.getEmail());
      existing.setWebsite(supplier.getWebsite());

      Ivy.repo().save(existing);
    } catch (Exception e) {
      Ivy.log().error(e);
    }
    return findById(supplier.getSupplierId());
  }

  /**
   * Deletes a supplier.
   *
   * @param supplier the supplier to delete, ignored if not exist
   */
  public void delete(Supplier supplier) {
    if (supplier == null) {
      return;
    }
    Supplier supplierInRepo = findById(supplier.getSupplierId());
    if (supplierInRepo != null) {
      Ivy.repo().delete(supplierInRepo);
    }
  }

  /**
   * Finds supplier by ID.
   *
   * @param id supplier identifier
   * @return supplier or null if not found
   */
  public Supplier findById(String id) {
    return Ivy.repo().search(Supplier.class).textField(FIELD_SUPPLIER_ID).isEqualToIgnoringCase(id).execute()
        .getFirst();
  }

  /**
   * Searches suppliers based on the provided criteria. Returns all suppliers if
   * criteria is null or has no filters.
   */
  public List<Supplier> findByCriteria(SupplierSearchCriteria criteria) {
    if (criteria == null || !criteria.hasAnyFilter()) {
      return Ivy.repo().search(Supplier.class).execute().getAll();
    }

    var search = Ivy.repo().search(Supplier.class);

    List<Filter<Supplier>> filters = new ArrayList<>();

    if (StringUtils.isNotBlank(criteria.getSupplierId())) {
      filters.add(search.textField(FIELD_SUPPLIER_ID).isEqualToIgnoringCase(criteria.getSupplierId()));
    }

    if (StringUtils.isNotBlank(criteria.getPhone())) {
      filters.add(search.textField(FIELD_PHONE).isEqualToIgnoringCase(criteria.getPhone()));
    }

    if (StringUtils.isNotBlank(criteria.getEmail())) {
      filters.add(search.textField(FIELD_EMAIL).isEqualToIgnoringCase(criteria.getEmail()));
    }

    if (StringUtils.isNotBlank(criteria.getWebsite())) {
      filters.add(search.textField(FIELD_WEBSITE).isEqualToIgnoringCase(criteria.getWebsite()));
    }

    // Apply collected filters
    for (Filter<Supplier> f : filters) {
      search.filter(f).or();
    }

    // Ranking results using score
    if (StringUtils.isNotBlank(criteria.getBusinessNameContains())) {
      search.score().textField(FIELD_BUSINESS_NAME).query(criteria.getBusinessNameContains().replace(" ", "+"))
          .limit(100);
    }

    if (criteria.getBusinessAddress() != null) {
      search.score().textField(FIELD_BUSINESS_ADDRESS).query(criteria.getBusinessAddress().toString().replace(" ", "+"))
          .limit(100);
    }

    return search.execute().getAll();
  }

  public Supplier findExactSupplier(SupplierSearchCriteria criteria) {
    if (criteria == null || !criteria.hasAnyFilter()) {
      return null;
    }

    var search = Ivy.repo().search(Supplier.class);

    List<Filter<Supplier>> filters = new ArrayList<>();

    if (StringUtils.isNotBlank(criteria.getSupplierId())) {
      filters.add(search.textField(FIELD_SUPPLIER_ID).isEqualToIgnoringCase(criteria.getSupplierId()));
    }

    if (StringUtils.isNotBlank(criteria.getBusinessNameContains())) {
      filters.add(search.textField(FIELD_BUSINESS_NAME).containsAllWordPatterns(criteria.getBusinessNameContains()));
    }

    // Apply collected filters
    for (Filter<Supplier> f : filters) {
      search.filter(f).or();
    }

    return search.execute().getFirst();
  }
}