package com.axonivy.utils.smart.workflow.demo.shopping.productcategory;

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

  /**
   * Creates or saves a new product category.
   *
   * @param category the product category to save
   * @return the persisted product category
   * @throws IllegalArgumentException if category is null
   */
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

  /**
   * Retrieves all product categories.
   *
   * @return list of all product categories
   */
  public List<ProductCategory> findAll() {
    return Ivy.repo().search(ProductCategory.class).execute().getAll();
  }

  /**
   * Updates an existing product category.
   *
   * @param category the product category to update
   * @return the updated product category
   */
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

  /**
   * Deletes a product category.
   *
   * @param category the product category to delete, ignored if not exist
   */
  public void delete(ProductCategory category) {
    if (category == null) {
      return;
    }
    ProductCategory categoryInRepo = findById(category.getCategoryId());
    if (categoryInRepo != null) {
      Ivy.repo().delete(categoryInRepo);
    }
  }

  /**
   * Finds product category by ID.
   *
   * @param id product category identifier
   * @return product category or null if not found
   */
  public ProductCategory findById(String id) {
    return Ivy.repo().search(ProductCategory.class).textField(FIELD_CATEGORY_ID).isEqualToIgnoringCase(id).execute()
        .getFirst();
  }

  /**
   * Searches product categories based on the provided criteria. Returns all
   * categories if criteria is null or has no filters.
   */
  public List<ProductCategory> findByCriteria(ProductCategorySearchCriteria criteria) {
    if (criteria == null || !criteria.hasAnyFilter()) {
      return Ivy.repo().search(ProductCategory.class).execute().getAll();
    }

    var search = Ivy.repo().search(ProductCategory.class);

    List<Filter<ProductCategory>> filters = new ArrayList<>();

    if (StringUtils.isNotBlank(criteria.getCategoryId())) {
      filters.add(search.textField(FIELD_CATEGORY_ID).isEqualToIgnoringCase(criteria.getCategoryId()));
    }

    // Apply collected filters
    for (Filter<ProductCategory> f : filters) {
      search.filter(f).or();
    }

    // Ranking results using score
    if (StringUtils.isNotBlank(criteria.getNameContains())) {
      search.score().textField(FIELD_NAME).query(prepareInputForScoreSearch(criteria.getNameContains())).limit(100);
    }

    if (StringUtils.isNotBlank(criteria.getDescriptionContains())) {
      search.score().textField(FIELD_DESCRIPTION).query(prepareInputForScoreSearch(criteria.getDescriptionContains()))
          .limit(100);
    }

    return search.execute().getAll();
  }

  /**
   * Finds product categories by name (partial match).
   *
   * @param name the category name to search for
   * @return list of categories matching the name pattern
   */
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

    // Apply collected filters
    for (Filter<ProductCategory> f : filters) {
      search.filter(f).or();
    }

    return search.execute().getFirst();
  }

  private String prepareInputForScoreSearch(String input) {
    return input.replaceAll(" ", "|").replaceAll("-", "|");
  }
}