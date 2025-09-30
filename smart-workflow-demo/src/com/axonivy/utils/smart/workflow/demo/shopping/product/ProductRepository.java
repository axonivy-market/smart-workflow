package com.axonivy.utils.smart.workflow.demo.shopping.product;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.smart.workflow.demo.shopping.brand.Brand;
import com.axonivy.utils.smart.workflow.demo.shopping.brand.BrandRepository;
import com.axonivy.utils.smart.workflow.demo.shopping.product.image.ProductImage;
import com.axonivy.utils.smart.workflow.demo.shopping.product.image.ProductImageRepository;
import com.axonivy.utils.smart.workflow.demo.shopping.productcategory.ProductCategory;
import com.axonivy.utils.smart.workflow.demo.shopping.productcategory.ProductCategoryRepository;
import com.axonivy.utils.smart.workflow.demo.shopping.supplier.Supplier;
import com.axonivy.utils.smart.workflow.demo.shopping.supplier.SupplierRepository;
import com.axonivy.utils.smart.workflow.utils.IdGenerationUtils;

import ch.ivyteam.ivy.business.data.store.search.Filter;
import ch.ivyteam.ivy.environment.Ivy;

public class ProductRepository {

  private static final String FIELD_PRODUCT_ID = "productId";
  private static final String FIELD_SKU = "sku";
  private static final String FIELD_NAME = "name";
  private static final String FIELD_DESCRIPTION = "description";
  private static final String FIELD_CATEGORY = "category";
  private static final String FIELD_BRAND = "brand";
  private static final String FIELD_UNIT_PRICE = "unitPrice";
  private static final String FIELD_ACTIVE = "active";

  private static ProductRepository instance;

  public static ProductRepository getInstance() {
    if (instance == null) {
      instance = new ProductRepository();
    }
    return instance;
  }

  /**
   * Creates or saves a new product.
   *
   * @param product the product to save
   * @return the persisted product
   * @throws IllegalArgumentException if product is null
   */
  public Product create(Product product) {
    if (product == null) {
      throw new IllegalArgumentException("Product cannot be null");
    }

    if (StringUtils.isBlank(product.getProductId())) {
      product.setProductId(IdGenerationUtils.generateRandomId());
    }

    product.setSupplierId(
        Optional.ofNullable(product).map(Product::getSupplier).map(Supplier::getSupplierId).orElse(StringUtils.EMPTY));
    product.setCategoryId(Optional.ofNullable(product).map(Product::getCategory).map(ProductCategory::getCategoryId)
        .orElse(StringUtils.EMPTY));
    product.setBrandId(
        Optional.ofNullable(product).map(Product::getBrand).map(Brand::getBrandId).orElse(StringUtils.EMPTY));
    if (StringUtils.isBlank(product.getImageId())) {
      product.setImageId(
          Optional.ofNullable(product).map(Product::getImage).map(ProductImage::getImageId).orElse(StringUtils.EMPTY));
    }

    product.setSupplier(null);
    product.setCategory(null);
    product.setBrand(null);
    product.setImage(null);

    Ivy.repo().save(product);
    return findById(product.getProductId());
  }

  public void createFromFile(Product product) {
    if (product == null) {
      return;
    }

    Ivy.repo().save(product);
  }

  /**
   * Retrieves all products.
   *
   * @return list of all products
   */
  public List<Product> findAll() {
    return loadRelatedDataForList(Ivy.repo().search(Product.class).execute().getAll());
  }

  /**
   * Updates an existing product.
   *
   * @param product the product to update
   * @return the updated product
   */
  public Product update(Product product) {
    if (product == null) {
      return null;
    }

    Product existing = findById(product.getProductId());

    try {
      existing.setName(product.getName());
      existing.setDescription(product.getDescription());
      existing.setSupplierId(product.getSupplier().getSupplierId());
      existing.setCategoryId(product.getCategory().getCategoryId());
      existing.setBrandId(product.getBrand().getBrandId());
      existing.setUnitPrice(product.getUnitPrice());
      existing.setActive(product.isActive());

      existing.setSupplier(null);
      existing.setCategory(null);
      existing.setBrand(null);

      Ivy.repo().save(existing);
    } catch (Exception e) {
      Ivy.log().error(e);
    }
    return findById(product.getProductId());
  }

  /**
   * Deletes a product.
   *
   * @param product the product to delete, ignored if not exist
   */
  public void delete(Product product) {
    if (product == null) {
      return;
    }
    Product productInRepo = findById(product.getProductId());
    if (productInRepo != null) {
      Ivy.repo().delete(productInRepo);
    }
  }

  /**
   * Finds product by ID.
   *
   * @param id product identifier
   * @return product or null if not found
   */
  public Product findById(String id) {
    return loadRelatedData(
        Ivy.repo().search(Product.class).textField(FIELD_PRODUCT_ID).isEqualToIgnoringCase(id).execute().getFirst());
  }

  /**
   * Searches products based on the provided criteria. Returns all products if
   * criteria is null or has no filters.
   */
  public List<Product> findByCriteria(ProductSearchCriteria criteria) {
    if (criteria == null || !criteria.hasAnyFilter()) {
      return loadRelatedDataForList(findAll());
    }

    var search = Ivy.repo().search(Product.class);

    List<Filter<Product>> filters = new ArrayList<>();

    if (StringUtils.isNotBlank(criteria.getProductId())) {
      filters.add(search.textField(FIELD_PRODUCT_ID).isEqualToIgnoringCase(criteria.getProductId()));
    }

    if (StringUtils.isNotBlank(criteria.getProductId())) {
      filters.add(search.textField(FIELD_SKU).isEqualToIgnoringCase(criteria.getSku()));
    }

    if (StringUtils.isNotBlank(criteria.getNameContains())) {
      filters.add(search.textField(FIELD_NAME).containsAllWordPatterns(criteria.getNameContains()));
    }

    if (StringUtils.isNotBlank(criteria.getDescriptionContains())) {
      filters.add(search.textField(FIELD_DESCRIPTION).containsAllWordPatterns(criteria.getDescriptionContains()));
    }

    if (criteria.getCategory() != null) {
      // Search by category object - assuming it has a searchable representation
      filters.add(search.textField(FIELD_CATEGORY).containsAllWordPatterns(criteria.getCategory().toString()));
    }

    if (StringUtils.isNotBlank(criteria.getCategoryId())) {
      // Search by category ID within the category field
      filters.add(search.textField(FIELD_CATEGORY + ".categoryId").isEqualToIgnoringCase(criteria.getCategoryId()));
    }

    if (criteria.getBrand() != null) {
      // Search by brand object - assuming it has a searchable representation
      filters.add(search.textField(FIELD_BRAND).containsAllWordPatterns(criteria.getBrand().toString()));
    }

    if (StringUtils.isNotBlank(criteria.getBrandId())) {
      // Search by brand ID within the brand field
      filters.add(search.textField(FIELD_BRAND + ".brandId").isEqualToIgnoringCase(criteria.getBrandId()));
    }

    if (criteria.getMaxUnitPrice() != null) {
      filters.add(search.numberField(FIELD_UNIT_PRICE).isLessOrEqualTo(criteria.getMaxUnitPrice()));
    }

    if (criteria.getMinUnitPrice() != null) {
      filters.add(search.numberField(FIELD_UNIT_PRICE).isGreaterOrEqualTo(criteria.getMinUnitPrice()));
    }

    // For active status, we can search for the string representation
    if (criteria.getActive() != null) {
      filters.add(search.textField(FIELD_ACTIVE).isEqualToIgnoringCase(criteria.getActive().toString()));
    }

    // Apply collected filters
    for (Filter<Product> f : filters) {
      search.filter(f).and();
    }

    return loadRelatedDataForList(search.execute().getAll());
  }

  public List<Product> findByCategories(List<String> categories) {
    if (CollectionUtils.isEmpty(categories)) {
      return null;
    }

    var search = Ivy.repo().search(Product.class);
    for (String category : categories) {
      search.textField("categoryId").containsAllWords(category).or();
    }

    return loadRelatedDataForList(search.execute().getAll());
  }

  public Product findSingleByCriteria(ProductSearchCriteria criteria) {
    List<Product> foundProducts = findByCriteria(criteria);
    if (CollectionUtils.isEmpty(foundProducts)) {
      return null;
    }
    return loadRelatedData(foundProducts.get(0));
  }

  /**
   * Finds products by category ID.
   *
   * @param categoryId the category identifier
   * @return list of products in the specified category
   */
  public List<Product> findByCategoryId(String categoryId) {
    if (StringUtils.isBlank(categoryId)) {
      return new ArrayList<>();
    }
    return loadRelatedDataForList(Ivy.repo().search(Product.class).textField(FIELD_CATEGORY + ".categoryId")
        .isEqualToIgnoringCase(categoryId).execute().getAll());
  }

  /**
   * Finds products by brand ID.
   *
   * @param brandId the brand identifier
   * @return list of products from the specified brand
   */
  public List<Product> findByBrandId(String brandId) {
    if (StringUtils.isBlank(brandId)) {
      return new ArrayList<>();
    }
    return loadRelatedDataForList(Ivy.repo().search(Product.class).textField(FIELD_BRAND + ".brandId")
        .isEqualToIgnoringCase(brandId).execute().getAll());
  }

  /**
   * Finds product by SKU
   *
   * @param sku the product SKU
   * @return matched product by SKU
   */
  public Product findBySku(String sku) {
    if (StringUtils.isBlank(sku)) {
      return null;
    }
    return loadRelatedData(
        Ivy.repo().search(Product.class).textField(FIELD_SKU).isEqualToIgnoringCase(sku).execute().getFirst());
  }

  /**
   * Finds active products only.
   *
   * @return list of active products
   */
  public List<Product> findActiveProducts() {
    return loadRelatedDataForList(
        Ivy.repo().search(Product.class).textField(FIELD_ACTIVE).isEqualToIgnoringCase("true").execute().getAll());
  }

  private Product loadRelatedData(Product product) {
    if (product == null) {
      return product;
    }

    if (StringUtils.isNotBlank(product.getSupplierId())) {
      product.setSupplier(SupplierRepository.getInstance().findById(product.getSupplierId()));
    }

    if (StringUtils.isNotBlank(product.getCategoryId())) {
      product.setCategory(ProductCategoryRepository.getInstance().findById(product.getCategoryId()));
    }

    if (StringUtils.isNotBlank(product.getBrandId())) {
      product.setBrand(BrandRepository.getInstance().findById(product.getBrandId()));
    }

    if (StringUtils.isNotBlank(product.getImageId())) {
      product.setImage(ProductImageRepository.getInstance().findById(product.getImageId()));
    }

    return product;
  }

  private List<Product> loadRelatedDataForList(List<Product> products) {
    if (CollectionUtils.isEmpty(products)) {
      return products;
    }

    products.forEach(this::loadRelatedData);
    return products;
  }
}