package com.axonivy.utils.smart.workflow.demo.shopping.brand;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

import ch.ivyteam.ivy.business.data.store.search.Filter;
import ch.ivyteam.ivy.business.data.store.search.Scored;
import ch.ivyteam.ivy.environment.Ivy;

public class BrandRepository {

  private static final String FIELD_BRAND_ID = "brandId";
  private static final String FIELD_NAME = "name";
  private static final String FIELD_DESCRIPTION = "description";
  private static final String FIELD_WEBSITE = "website";

  private static BrandRepository instance;

  public static BrandRepository getInstance() {
    if (instance == null) {
      instance = new BrandRepository();
    }
    return instance;
  }

  /**
   * Creates or saves a new brand.
   *
   * @param brand the brand to save
   * @return the persisted brand
   * @throws IllegalArgumentException if brand is null
   */
  public Brand create(Brand brand) {
    if (brand == null) {
      throw new IllegalArgumentException("Brand cannot be null");
    }

    if (StringUtils.isBlank(brand.getBrandId())) {
      brand.setBrandId(UUID.randomUUID().toString());
    }

    Ivy.repo().save(brand);
    return brand;
  }

  /**
   * Retrieves all brands.
   *
   * @return list of all brands
   */
  public List<Brand> findAll() {
    return Ivy.repo().search(Brand.class).execute().getAll();
  }

  /**
   * Updates an existing brand.
   *
   * @param brand the brand to update
   * @return the updated brand
   */
  public Brand update(Brand brand) {
    if (brand == null) {
      return null;
    }

    Brand existing = findById(brand.getBrandId());

    try {
      existing.setName(brand.getName());
      existing.setDescription(brand.getDescription());
      existing.setLogoBase64(brand.getLogoBase64());
      existing.setWebsite(brand.getWebsite());

      Ivy.repo().save(existing);
    } catch (Exception e) {
      Ivy.log().error(e);
    }
    return findById(brand.getBrandId());
  }

  /**
   * Deletes a brand.
   *
   * @param brand the brand to delete, ignored if not exist
   */
  public void delete(Brand brand) {
    if (brand == null) {
      return;
    }
    Brand brandInRepo = findById(brand.getBrandId());
    if (brandInRepo != null) {
      Ivy.repo().delete(brandInRepo);
    }
  }

  /**
   * Finds brand by ID.
   *
   * @param id brand identifier
   * @return brand or null if not found
   */
  public Brand findById(String id) {
    return Ivy.repo().search(Brand.class).textField(FIELD_BRAND_ID).isEqualToIgnoringCase(id).execute().getFirst();
  }

  private List<Brand> mapScore(List<Scored<Brand>> withScores) {
    List<Brand> result = new ArrayList<>();

    for (int i = 0; i < withScores.size(); i++) {
      var brand = withScores.get(i);
      brand.getValue().setMatchingScore(brand.getScore());
      result.add(brand.getValue());
    }

    return result;
  }

  /**
   * Searches brands based on the provided criteria. Returns all brands if
   * criteria is null or has no filters.
   */
  public List<Brand> findByCriteria(BrandSearchCriteria criteria) {
    if (criteria == null || !criteria.hasAnyFilter()) {
      return mapScore(Ivy.repo().search(Brand.class).execute().getAllWithScore());
    }

    var search = Ivy.repo().search(Brand.class);

    List<Filter<Brand>> filters = new ArrayList<>();

    if (StringUtils.isNotBlank(criteria.getBrandId())) {
      filters.add(search.textField(FIELD_BRAND_ID).isEqualToIgnoringCase(criteria.getBrandId()));
    }

    if (StringUtils.isNotBlank(criteria.getWebsite())) {
      filters.add(search.textField(FIELD_WEBSITE).isEqualToIgnoringCase(criteria.getWebsite()));
    }

    // Apply collected filters
    for (Filter<Brand> f : filters) {
      search.filter(f).or();
    }

    // Ranking results using score
    if (StringUtils.isNotBlank(criteria.getNameContains())) {
      search.score().textField(FIELD_NAME).query(criteria.getNameContains().replace(" ", "+")).limit(100);
    }

    if (StringUtils.isNotBlank(criteria.getDescriptionContains())) {
      search.score().textField(FIELD_DESCRIPTION).query(criteria.getDescriptionContains().replace(" ", "+")).limit(100);
    }

    return mapScore(search.execute().getAllWithScore());
  }

  public Brand findExactBrand(BrandSearchCriteria criteria) {
    if (criteria == null || !criteria.hasAnyFilter()) {
      return null;
    }

    var search = Ivy.repo().search(Brand.class);

    List<Filter<Brand>> filters = new ArrayList<>();

    if (StringUtils.isNotBlank(criteria.getBrandId())) {
      filters.add(search.textField(FIELD_BRAND_ID).isEqualToIgnoringCase(criteria.getBrandId()));
    }

    if (StringUtils.isNotBlank(criteria.getNameContains())) {
      filters.add(search.textField(FIELD_NAME).containsAllWordPatterns(criteria.getNameContains()));
    }

    // Apply collected filters
    for (Filter<Brand> f : filters) {
      search.filter(f).or();
    }

    return search.execute().getFirst();
  }
}