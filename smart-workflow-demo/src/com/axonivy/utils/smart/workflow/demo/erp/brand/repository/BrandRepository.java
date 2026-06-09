package com.axonivy.utils.smart.workflow.demo.erp.brand.repository;

import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.smart.workflow.demo.erp.brand.model.Brand;
import com.axonivy.utils.smart.workflow.demo.erp.shared.BusinessDataStore;
import com.axonivy.utils.smart.workflow.demo.erp.shared.IvyBusinessDataStore;

public class BrandRepository {

  private static final String FIELD_BRAND_ID = "brandId";
  private static final String FIELD_NAME = "name";
  private static final String FIELD_WEBSITE = "website";

  private static BrandRepository instance;

  private final BusinessDataStore store;

  public BrandRepository() {
    this(IvyBusinessDataStore.getInstance());
  }

  public BrandRepository(BusinessDataStore store) {
    this.store = store;
  }

  public static BrandRepository getInstance() {
    if (instance == null) {
      instance = new BrandRepository();
    }
    return instance;
  }

  public Brand create(Brand brand) {
    if (brand == null) {
      throw new IllegalArgumentException("Brand cannot be null");
    }

    if (StringUtils.isBlank(brand.getBrandId())) {
      brand.setBrandId(UUID.randomUUID().toString());
    }

    store.save(brand);
    return brand;
  }

  public List<Brand> findAll() {
    return store.findAll(Brand.class);
  }

  public Brand update(Brand brand) {
    if (brand == null) {
      return null;
    }

    Brand existing = findById(brand.getBrandId());
    if (existing == null) {
      return null;
    }

    existing.setName(brand.getName());
    existing.setDescription(brand.getDescription());
    existing.setLogoBase64(brand.getLogoBase64());
    existing.setWebsite(brand.getWebsite());

    store.save(existing);
    return findById(brand.getBrandId());
  }

  public void delete(Brand brand) {
    if (brand == null) {
      return;
    }
    Brand brandInRepo = findById(brand.getBrandId());
    if (brandInRepo != null) {
      store.delete(brandInRepo);
    }
  }

  public Brand findById(String id) {
    return store.findFirstByField(Brand.class, FIELD_BRAND_ID, id);
  }

  public List<Brand> findByCriteria(BrandSearchCriteria criteria) {
    if (criteria == null || !criteria.hasAnyFilter()) {
      return store.findAll(Brand.class);
    }

    if (StringUtils.isNotBlank(criteria.getBrandId())) {
      return store.findByField(Brand.class, FIELD_BRAND_ID, criteria.getBrandId());
    }

    if (StringUtils.isNotBlank(criteria.getWebsite())) {
      return store.findByField(Brand.class, FIELD_WEBSITE, criteria.getWebsite());
    }

    if (StringUtils.isNotBlank(criteria.getNameContains())) {
      return store.findByFieldContaining(Brand.class, FIELD_NAME, criteria.getNameContains());
    }

    return store.findAll(Brand.class);
  }

  public Brand findExactBrand(BrandSearchCriteria criteria) {
    if (criteria == null || !criteria.hasAnyFilter()) {
      return null;
    }

    if (StringUtils.isNotBlank(criteria.getBrandId())) {
      return store.findFirstByField(Brand.class, FIELD_BRAND_ID, criteria.getBrandId());
    }

    if (StringUtils.isNotBlank(criteria.getNameContains())) {
      List<Brand> results = store.findByFieldContaining(Brand.class, FIELD_NAME, criteria.getNameContains());
      return results.isEmpty() ? null : results.get(0);
    }

    return null;
  }
}
