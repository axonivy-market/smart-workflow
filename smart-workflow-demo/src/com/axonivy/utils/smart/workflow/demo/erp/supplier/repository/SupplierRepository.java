package com.axonivy.utils.smart.workflow.demo.erp.supplier.repository;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.smart.workflow.demo.erp.supplier.model.Supplier;
import com.axonivy.utils.smart.workflow.utils.IdGenerationUtils;

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

  public void delete(Supplier supplier) {
    if (supplier == null) {
      return;
    }
    Supplier supplierInRepo = findById(supplier.getSupplierId());
    if (supplierInRepo != null) {
      Ivy.repo().delete(supplierInRepo);
    }
  }

  public Supplier findById(String id) {
    return Ivy.repo().search(Supplier.class).textField(FIELD_SUPPLIER_ID).isEqualToIgnoringCase(id).execute()
        .getFirst();
  }

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

    for (Filter<Supplier> f : filters) {
      search.filter(f).or();
    }

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

    for (Filter<Supplier> f : filters) {
      search.filter(f).or();
    }

    return search.execute().getFirst();
  }
}