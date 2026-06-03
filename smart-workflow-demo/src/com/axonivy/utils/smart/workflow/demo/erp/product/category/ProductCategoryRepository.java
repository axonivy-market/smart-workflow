package com.axonivy.utils.smart.workflow.demo.erp.product.category;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.smart.workflow.utils.IdGenerationUtils;

import ch.ivyteam.ivy.business.data.store.search.Filter;
import ch.ivyteam.ivy.environment.Ivy;

public class ProductCategoryRepository {

  private static final String FIELD_CATEGORY_ID = "categoryId";
  private static final String FIELD_NAME = "name";
  private static final String FIELD_DESCRIPTION = "description";

  private static ProductCategoryRepository instance;

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

    Ivy.repo().save(category);
    return category;
  }

  public List<ProductCategory> findAll() {
    return Ivy.repo().search(ProductCategory.class).execute().getAll();
  }

  public ProductCategory update(ProductCategory category) {
    if (category == null) {
      return null;
    }

    ProductCategory existing = findById(category.getCategoryId());

    try {
      existing.setName(category.getName());
      existing.setDescription(category.getDescription());

      Ivy.repo().save(existing);
    } catch (Exception e) {
      Ivy.log().error(e);
    }
    return findById(category.getCategoryId());
  }

  public void delete(ProductCategory category) {
    if (category == null) {
      return;
    }
    ProductCategory categoryInRepo = findById(category.getCategoryId());
    if (categoryInRepo != null) {
      Ivy.repo().delete(categoryInRepo);
    }
  }

  public ProductCategory findById(String id) {
    return Ivy.repo().search(ProductCategory.class).textField(FIELD_CATEGORY_ID).isEqualToIgnoringCase(id).execute()
        .getFirst();
  }

  public List<ProductCategory> findByCriteria(ProductCategorySearchCriteria criteria) {
    if (criteria == null || !criteria.hasAnyFilter()) {
      return Ivy.repo().search(ProductCategory.class).execute().getAll();
    }

    var search = Ivy.repo().search(ProductCategory.class);

    List<Filter<ProductCategory>> filters = new ArrayList<>();

    if (StringUtils.isNotBlank(criteria.getCategoryId())) {
      filters.add(search.textField(FIELD_CATEGORY_ID).isEqualToIgnoringCase(criteria.getCategoryId()));
    }

    for (Filter<ProductCategory> f : filters) {
      search.filter(f).or();
    }

    if (StringUtils.isNotBlank(criteria.getNameContains())) {
      search.score().textField(FIELD_NAME).query(prepareInputForScoreSearch(criteria.getNameContains())).limit(100);
    }

    if (StringUtils.isNotBlank(criteria.getDescriptionContains())) {
      search.score().textField(FIELD_DESCRIPTION).query(prepareInputForScoreSearch(criteria.getDescriptionContains()))
          .limit(100);
    }

    return search.execute().getAll();
  }

  public List<ProductCategory> findByNameContaining(String name) {
    if (StringUtils.isBlank(name)) {
      return new ArrayList<>();
    }
    return Ivy.repo().search(ProductCategory.class).textField(FIELD_NAME).containsAllWordPatterns(name).execute()
        .getAll();
  }

  public ProductCategory findExactProductCategory(ProductCategorySearchCriteria criteria) {
    if (criteria == null || !criteria.hasAnyFilter()) {
      return null;
    }

    var search = Ivy.repo().search(ProductCategory.class);

    List<Filter<ProductCategory>> filters = new ArrayList<>();

    if (StringUtils.isNotBlank(criteria.getCategoryId())) {
      filters.add(search.textField(FIELD_CATEGORY_ID).isEqualToIgnoringCase(criteria.getCategoryId()));
    }

    if (StringUtils.isNotBlank(criteria.getNameContains())) {
      filters.add(search.textField(FIELD_NAME).containsAllWordPatterns(criteria.getNameContains()));
    }

    for (Filter<ProductCategory> f : filters) {
      search.filter(f).or();
    }

    return search.execute().getFirst();
  }

  private String prepareInputForScoreSearch(String input) {
    return input.replaceAll(" ", "|").replaceAll("-", "|");
  }
}