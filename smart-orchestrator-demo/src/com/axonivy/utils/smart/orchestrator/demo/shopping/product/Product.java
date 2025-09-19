package com.axonivy.utils.smart.orchestrator.demo.shopping.product;

import java.io.Serializable;
import java.util.List;

import com.axonivy.utils.smart.orchestrator.demo.shopping.brand.Brand;
import com.axonivy.utils.smart.orchestrator.demo.shopping.common.OtherInformation;
import com.axonivy.utils.smart.orchestrator.demo.shopping.product.image.ProductImage;
import com.axonivy.utils.smart.orchestrator.demo.shopping.productcategory.ProductCategory;
import com.axonivy.utils.smart.orchestrator.demo.shopping.supplier.Supplier;
import com.axonivy.utils.smart.orchestrator.demo.shopping.utils.ImageUtils;
import com.fasterxml.jackson.annotation.JsonIgnore;

import dev.langchain4j.model.output.structured.Description;

public class Product implements Serializable {

  private static final long serialVersionUID = 4773733364138475983L;

  @Description("Unique product identifier")
  private String productId;

  @Description("SKU number")
  private String sku;

  @Description("Details of this product's supplier")
  private Supplier supplier;

  @Description("Name of the product")
  private String name;

  @Description("Detailed description of the product")
  private String description;

  @Description("Category to which the product belongs")
  private ProductCategory category;

  @Description("Brand associated with the product")
  private Brand brand;

  @Description("Unit price of the product as a string (e.g., '19.99')")
  private Float unitPrice;

  @Description("Indicates if the product is currently active/available")
  private boolean active;

  @JsonIgnore
  @Description("Image of the product. Skip using this field")
  private ProductImage image;

  @Description("Image of the product. Skip using this field")
  private String imageId;

  @Description("Other information of the product. Each field is a pair of label/value")
  private List<OtherInformation> otherInformations;

  private String supplierId;

  private String categoryId;

  private String brandId;

  public String getProductId() {
    return productId;
  }

  public void setProductId(String productId) {
    this.productId = productId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public ProductCategory getCategory() {
    return category;
  }

  public void setCategory(ProductCategory category) {
    this.category = category;
  }

  public Brand getBrand() {
    return brand;
  }

  public void setBrand(Brand brand) {
    this.brand = brand;
  }

  public Float getUnitPrice() {
    return unitPrice;
  }

  public void setUnitPrice(Float unitPrice) {
    this.unitPrice = unitPrice;
  }

  public boolean isActive() {
    return active;
  }

  public void setActive(boolean active) {
    this.active = active;
  }

  public String getSku() {
    return sku;
  }

  public void setSku(String sku) {
    this.sku = sku;
  }

  public List<OtherInformation> getOtherInformations() {
    return otherInformations;
  }

  public void setOtherInformations(List<OtherInformation> otherInformations) {
    this.otherInformations = otherInformations;
  }

  public Supplier getSupplier() {
    return supplier;
  }

  public void setSupplier(Supplier supplier) {
    this.supplier = supplier;
  }

  public String getSupplierId() {
    return supplierId;
  }

  public void setSupplierId(String supplierId) {
    this.supplierId = supplierId;
  }

  public String getCategoryId() {
    return categoryId;
  }

  public void setCategoryId(String categoryId) {
    this.categoryId = categoryId;
  }

  public String getBrandId() {
    return brandId;
  }

  public void setBrandId(String brandId) {
    this.brandId = brandId;
  }

  public ProductImage getImage() {
    return image;
  }

  public void setImage(ProductImage image) {
    this.image = image;
  }

  @JsonIgnore
  public String getImageUrl() {
    return ImageUtils.base64ToDataUrl(this.image.getContent());
  }

  public String getImageId() {
    return imageId;
  }

  public void setImageId(String imageId) {
    this.imageId = imageId;
  }
}