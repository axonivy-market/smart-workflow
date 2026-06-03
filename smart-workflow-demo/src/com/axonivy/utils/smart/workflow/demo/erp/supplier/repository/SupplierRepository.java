package com.axonivy.utils.smart.workflow.demo.erp.supplier.repository;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.smart.workflow.demo.erp.shared.BusinessDataStore;
import com.axonivy.utils.smart.workflow.demo.erp.shared.IvyBusinessDataStore;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.model.Supplier;
import com.axonivy.utils.smart.workflow.utils.IdGenerationUtils;

public class SupplierRepository {

  private static final String FIELD_SUPPLIER_ID = "supplierId";
  private static final String FIELD_BUSINESS_NAME = "businessName";
  private static final String FIELD_PHONE = "phone";
  private static final String FIELD_EMAIL = "email";
  private static final String FIELD_WEBSITE = "website";

  private static SupplierRepository instance;

  private final BusinessDataStore store;

  public SupplierRepository() {
    this(IvyBusinessDataStore.getInstance());
  }

  public SupplierRepository(BusinessDataStore store) {
    this.store = store;
  }

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

    store.save(supplier);
    return supplier;
  }

  public List<Supplier> findAll() {
    return store.findAll(Supplier.class);
  }

  public Supplier update(Supplier supplier) {
    if (supplier == null) {
      return null;
    }

    Supplier existing = findById(supplier.getSupplierId());
    if (existing == null) {
      return null;
    }

    existing.setBusinessName(supplier.getBusinessName());
    existing.setBusinessAddress(supplier.getBusinessAddress());
    existing.setPhone(supplier.getPhone());
    existing.setEmail(supplier.getEmail());
    existing.setWebsite(supplier.getWebsite());

    store.save(existing);
    return findById(supplier.getSupplierId());
  }

  public void delete(Supplier supplier) {
    if (supplier == null) {
      return;
    }
    Supplier supplierInRepo = findById(supplier.getSupplierId());
    if (supplierInRepo != null) {
      store.delete(supplierInRepo);
    }
  }

  public Supplier findById(String id) {
    return store.findFirstByField(Supplier.class, FIELD_SUPPLIER_ID, id);
  }

  public List<Supplier> findByCriteria(SupplierSearchCriteria criteria) {
    if (criteria == null || !criteria.hasAnyFilter()) {
      return store.findAll(Supplier.class);
    }

    if (StringUtils.isNotBlank(criteria.getSupplierId())) {
      return store.findByField(Supplier.class, FIELD_SUPPLIER_ID, criteria.getSupplierId());
    }

    if (StringUtils.isNotBlank(criteria.getPhone())) {
      return store.findByField(Supplier.class, FIELD_PHONE, criteria.getPhone());
    }

    if (StringUtils.isNotBlank(criteria.getEmail())) {
      return store.findByField(Supplier.class, FIELD_EMAIL, criteria.getEmail());
    }

    if (StringUtils.isNotBlank(criteria.getWebsite())) {
      return store.findByField(Supplier.class, FIELD_WEBSITE, criteria.getWebsite());
    }

    if (StringUtils.isNotBlank(criteria.getBusinessNameContains())) {
      return store.findByFieldContaining(Supplier.class, FIELD_BUSINESS_NAME, criteria.getBusinessNameContains());
    }

    return store.findAll(Supplier.class);
  }

  public Supplier findExactSupplier(SupplierSearchCriteria criteria) {
    if (criteria == null || !criteria.hasAnyFilter()) {
      return null;
    }

    if (StringUtils.isNotBlank(criteria.getSupplierId())) {
      return store.findFirstByField(Supplier.class, FIELD_SUPPLIER_ID, criteria.getSupplierId());
    }

    if (StringUtils.isNotBlank(criteria.getBusinessNameContains())) {
      List<Supplier> results = store.findByFieldContaining(Supplier.class, FIELD_BUSINESS_NAME,
          criteria.getBusinessNameContains());
      return results.isEmpty() ? null : results.get(0);
    }

    return null;
  }
}
