package com.axonivy.utils.smart.orchestrator.demo.shopping.product;

import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.smart.orchestrator.demo.shopping.brand.Brand;
import com.axonivy.utils.smart.orchestrator.demo.shopping.productcategory.ProductCategory;

import dev.langchain4j.internal.Json;
import dev.langchain4j.model.output.structured.Description;

public class ProductSearchCriteria {

  @Description("Unique product identifier. Leave null if no filter is needed.")
  private String productId;

  @Description("SKU number of the product")
  private String sku;

  @Description("Product name filter (case-insensitive partial match). Leave null if not filtering by product name.")
  private String nameContains;

  @Description("Product description filter (case-insensitive partial match). Leave null if not filtering by description.")
  private String descriptionContains;

  @Description("Product category filter. Leave null if not filtering by category.")
  private ProductCategory category;

  @Description("Category ID filter (exact match). Leave null if not filtering by category ID.")
  private String categoryId;

  @Description("Brand filter. Leave null if not filtering by brand.")
  private Brand brand;

  @Description("Brand ID filter (exact match). Leave null if not filtering by brand ID.")
  private String brandId;

  @Description("Unit price filter (exact match). Leave null if not filtering by price.")
  private String unitPrice;

  @Description("Minimum unit price filter. Leave null if not filtering by minimum price.")
  private Float minUnitPrice;

  @Description("Maximum unit price filter. Leave null if not filtering by maximum price.")
  private Float maxUnitPrice;

  @Description("Active status filter. Leave null if not filtering by active status.")
  private Boolean active;

  public ProductSearchCriteria() {
  }

  // Getters and setters
  public String getProductId() {
    return productId;
  }

  public void setProductId(String productId) {
    this.productId = productId;
  }

  public String getNameContains() {
    return nameContains;
  }

  public void setNameContains(String nameContains) {
    this.nameContains = nameContains;
  }

  public String getDescriptionContains() {
    return descriptionContains;
  }

  public void setDescriptionContains(String descriptionContains) {
    this.descriptionContains = descriptionContains;
  }

  public ProductCategory getCategory() {
    return category;
  }

  public void setCategory(ProductCategory category) {
    this.category = category;
  }

  public String getCategoryId() {
    return categoryId;
  }

  public void setCategoryId(String categoryId) {
    this.categoryId = categoryId;
  }

  public Brand getBrand() {
    return brand;
  }

  public void setBrand(Brand brand) {
    this.brand = brand;
  }

  public String getBrandId() {
    return brandId;
  }

  public void setBrandId(String brandId) {
    this.brandId = brandId;
  }

  public String getUnitPrice() {
    return unitPrice;
  }

  public void setUnitPrice(String unitPrice) {
    this.unitPrice = unitPrice;
  }

  public Float getMinUnitPrice() {
    return minUnitPrice;
  }

  public void setMinUnitPrice(Float minUnitPrice) {
    this.minUnitPrice = minUnitPrice;
  }

  public Float getMaxUnitPrice() {
    return maxUnitPrice;
  }

  public void setMaxUnitPrice(Float maxUnitPrice) {
    this.maxUnitPrice = maxUnitPrice;
  }

  public Boolean getActive() {
    return active;
  }

  public void setActive(Boolean active) {
    this.active = active;
  }

  public String getSku() {
    return sku;
  }

  public void setSku(String sku) {
    this.sku = sku;
  }

  /**
   * Checks if this search criteria has any filters set.
   */
  public boolean hasAnyFilter() {
    return StringUtils.isNotBlank(productId) || StringUtils.isNotBlank(nameContains) 
        || StringUtils.isNotBlank(descriptionContains) || category != null 
        || StringUtils.isNotBlank(categoryId) || brand != null 
        || StringUtils.isNotBlank(brandId) || StringUtils.isNotBlank(unitPrice)
        || minUnitPrice != null || maxUnitPrice != null
        || active != null;
  }

  @Override
  public String toString() {
    return Json.toJson(this);
  }
}