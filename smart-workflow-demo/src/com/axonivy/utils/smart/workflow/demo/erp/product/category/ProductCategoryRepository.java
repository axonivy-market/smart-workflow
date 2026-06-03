package com.axonivy.utils.smart.workflow.demo.erp.product.category;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.smart.workflow.demo.erp.shared.BusinessDataStore;
import com.axonivy.utils.smart.workflow.demo.erp.shared.IvyBusinessDataStore;
import com.axonivy.utils.smart.workflow.utils.IdGenerationUtils;

public class ProductCategoryRepository {

  private static final String FIELD_CATEGORY_ID = "categoryId";
  private static final String FIELD_NAME = "name";
  private static final String FIELD_DESCRIPTION = "description";

  private static ProductCategoryRepository instance;

  private final BusinessDataStore store;

  public ProductCategoryRepository() {
    this(IvyBusinessDataStore.getInstance());
  }

  public ProductCategoryRepository(BusinessDataStore store) {
    this.store = store;
  }

  public static ProductCategoryRepository getInstance() {
    if (instance == null) {
      instance = new ProductCategoryRepository();
    }
    return instance;
  }

  public ProductCategory create(ProductCategory category) {
    if (category == null) {
      throw new IllegalArgumentException("ProductCategory cannot be null");
    }

    if (StringUtils.isBlank(category.getCategoryId())) {
      category.setCategoryId(IdGenerationUtils.generateRandomId());
    }

    store.save(category);
    return category;
  }

  public List<ProductCategory> findAll() {
    return store.findAll(ProductCategory.class);
  }

  public ProductCategory update(ProductCategory category) {
    if (category == null) {
      return null;
    }

    ProductCategory existing = findById(category.getCategoryId());
    if (existing == null) {
      return null;
    }

    existing.setName(category.getName());
    existing.setDescription(category.getDescription());

    store.save(existing);
    return findById(category.getCategoryId());
  }

  public void delete(ProductCategory category) {
    if (category == null) {
      return;
    }
    ProductCategory categoryInRepo = findById(category.getCategoryId());
    if (categoryInRepo != null) {
      store.delete(categoryInRepo);
    }
  }

  public ProductCategory findById(String id) {
    return store.findFirstByField(ProductCategory.class, FIELD_CATEGORY_ID, id);
  }

  public List<ProductCategory> findByCriteria(ProductCategorySearchCriteria criteria) {
    if (criteria == null || !criteria.hasAnyFilter()) {
      return store.findAll(ProductCategory.class);
    }

    if (StringUtils.isNotBlank(criteria.getCategoryId())) {
      return store.findByField(ProductCategory.class, FIELD_CATEGORY_ID, criteria.getCategoryId());
    }

    if (StringUtils.isNotBlank(criteria.getNameContains())) {
      return store.findByFieldContaining(ProductCategory.class, FIELD_NAME, criteria.getNameContains());
    }

    if (StringUtils.isNotBlank(criteria.getDescriptionContains())) {
      return store.findByFieldContaining(ProductCategory.class, FIELD_DESCRIPTION, criteria.getDescriptionContains());
    }

    return store.findAll(ProductCategory.class);
  }

  public List<ProductCategory> findByNameContaining(String name) {
    if (StringUtils.isBlank(name)) {
      return new ArrayList<>();
    }
    return store.findByFieldContaining(ProductCategory.class, FIELD_NAME, name);
  }

  public ProductCategory findExactProductCategory(ProductCategorySearchCriteria criteria) {
    if (criteria == null || !criteria.hasAnyFilter()) {
      return null;
    }

    if (StringUtils.isNotBlank(criteria.getCategoryId())) {
      return store.findFirstByField(ProductCategory.class, FIELD_CATEGORY_ID, criteria.getCategoryId());
    }

    if (StringUtils.isNotBlank(criteria.getNameContains())) {
      List<ProductCategory> results = store.findByFieldContaining(ProductCategory.class, FIELD_NAME,
          criteria.getNameContains());
      return results.isEmpty() ? null : results.get(0);
    }

    return null;
  }
}
