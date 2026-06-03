package com.axonivy.utils.smart.workflow.demo.erp.product.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.smart.workflow.demo.erp.brand.model.Brand;
import com.axonivy.utils.smart.workflow.demo.erp.brand.repository.BrandRepository;
import com.axonivy.utils.smart.workflow.demo.erp.product.category.ProductCategory;
import com.axonivy.utils.smart.workflow.demo.erp.product.category.ProductCategoryRepository;
import com.axonivy.utils.smart.workflow.demo.erp.product.model.Product;
import com.axonivy.utils.smart.workflow.demo.erp.product.model.ProductImage;
import com.axonivy.utils.smart.workflow.demo.erp.shared.BusinessDataStore;
import com.axonivy.utils.smart.workflow.demo.erp.shared.IvyBusinessDataStore;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.model.Supplier;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.repository.SupplierRepository;
import com.axonivy.utils.smart.workflow.utils.IdGenerationUtils;

public class ProductRepository {

  private static final String FIELD_PRODUCT_ID = "productId";
  private static final String FIELD_SKU = "sku";
  private static final String FIELD_NAME = "name";
  private static final String FIELD_DESCRIPTION = "description";
  private static final String FIELD_ACTIVE = "active";

  private static ProductRepository instance;

  private final BusinessDataStore store;

  public ProductRepository() {
    this(IvyBusinessDataStore.getInstance());
  }

  public ProductRepository(BusinessDataStore store) {
    this.store = store;
  }

  public static ProductRepository getInstance() {
    if (instance == null) {
      instance = new ProductRepository();
    }
    return instance;
  }

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

    store.save(product);
    return findById(product.getProductId());
  }

  public void createFromFile(Product product) {
    if (product == null) {
      return;
    }
    store.save(product);
  }

  public List<Product> findAll() {
    return loadRelatedDataForList(store.findAll(Product.class));
  }

  public Product update(Product product) {
    if (product == null) {
      return null;
    }

    Product existing = findById(product.getProductId());
    if (existing == null) {
      return null;
    }

    existing.setName(product.getName());
    existing.setDescription(product.getDescription());
    existing.setSupplierId(product.getSupplier() != null ? product.getSupplier().getSupplierId() : existing.getSupplierId());
    existing.setCategoryId(product.getCategory() != null ? product.getCategory().getCategoryId() : existing.getCategoryId());
    existing.setBrandId(product.getBrand() != null ? product.getBrand().getBrandId() : existing.getBrandId());
    existing.setUnitPrice(product.getUnitPrice());
    existing.setActive(product.isActive());

    existing.setSupplier(null);
    existing.setCategory(null);
    existing.setBrand(null);

    store.save(existing);
    return findById(product.getProductId());
  }

  public void delete(Product product) {
    if (product == null) {
      return;
    }
    Product productInRepo = findById(product.getProductId());
    if (productInRepo != null) {
      store.delete(productInRepo);
    }
  }

  public Product findById(String id) {
    return loadRelatedData(store.findFirstByField(Product.class, FIELD_PRODUCT_ID, id));
  }

  public List<Product> findByCriteria(ProductSearchCriteria criteria) {
    if (criteria == null || !criteria.hasAnyFilter()) {
      return loadRelatedDataForList(findAll());
    }

    if (StringUtils.isNotBlank(criteria.getProductId())) {
      return loadRelatedDataForList(store.findByField(Product.class, FIELD_PRODUCT_ID, criteria.getProductId()));
    }

    if (StringUtils.isNotBlank(criteria.getSku())) {
      return loadRelatedDataForList(store.findByField(Product.class, FIELD_SKU, criteria.getSku()));
    }

    if (StringUtils.isNotBlank(criteria.getNameContains())) {
      return loadRelatedDataForList(store.findByFieldContaining(Product.class, FIELD_NAME, criteria.getNameContains()));
    }

    if (StringUtils.isNotBlank(criteria.getDescriptionContains())) {
      return loadRelatedDataForList(
          store.findByFieldContaining(Product.class, FIELD_DESCRIPTION, criteria.getDescriptionContains()));
    }

    if (criteria.getActive() != null) {
      return loadRelatedDataForList(
          store.findByField(Product.class, FIELD_ACTIVE, criteria.getActive().toString()));
    }

    return loadRelatedDataForList(store.findAll(Product.class));
  }

  public List<Product> findByCategories(List<String> categories) {
    if (CollectionUtils.isEmpty(categories)) {
      return null;
    }

    List<Product> result = new ArrayList<>();
    for (String category : categories) {
      result.addAll(store.findByField(Product.class, "categoryId", category));
    }
    return loadRelatedDataForList(result);
  }

  public Product findSingleByCriteria(ProductSearchCriteria criteria) {
    List<Product> foundProducts = findByCriteria(criteria);
    if (CollectionUtils.isEmpty(foundProducts)) {
      return null;
    }
    return loadRelatedData(foundProducts.get(0));
  }

  public List<Product> findByCategoryId(String categoryId) {
    if (StringUtils.isBlank(categoryId)) {
      return new ArrayList<>();
    }
    return loadRelatedDataForList(store.findByField(Product.class, "categoryId", categoryId));
  }

  public List<Product> findByBrandId(String brandId) {
    if (StringUtils.isBlank(brandId)) {
      return new ArrayList<>();
    }
    return loadRelatedDataForList(store.findByField(Product.class, "brandId", brandId));
  }

  public Product findBySku(String sku) {
    if (StringUtils.isBlank(sku)) {
      return null;
    }
    return loadRelatedData(store.findFirstByField(Product.class, FIELD_SKU, sku));
  }

  public List<Product> findActiveProducts() {
    return loadRelatedDataForList(store.findByField(Product.class, FIELD_ACTIVE, "true"));
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
