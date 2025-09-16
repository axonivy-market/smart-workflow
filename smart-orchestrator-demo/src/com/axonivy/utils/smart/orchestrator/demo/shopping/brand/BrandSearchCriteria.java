package com.axonivy.utils.smart.orchestrator.demo.shopping.brand;

import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.smart.orchestrator.demo.shopping.supplier.Supplier;

import dev.langchain4j.internal.Json;
import dev.langchain4j.model.output.structured.Description;

public class BrandSearchCriteria {

  @Description("Unique brand identifier. Leave null if no filter is needed.")
  private String brandId;

  @Description("Brand name filter (case-insensitive partial match). Leave null if not filtering by brand name.")
  private String nameContains;

  @Description("Brand description filter (case-insensitive partial match). Leave null if not filtering by description.")
  private String descriptionContains;

  @Description("Website URL filter (exact match). Leave null if not filtering by website.")
  private String website;

  @Description("Supplier associated with this brand. Leave null if not filtering by supplier.")
  private Supplier supplier;

  public BrandSearchCriteria() {
  }

  // Getters and setters
  public String getBrandId() {
    return brandId;
  }

  public void setBrandId(String brandId) {
    this.brandId = brandId;
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

  public String getWebsite() {
    return website;
  }

  public void setWebsite(String website) {
    this.website = website;
  }

  public Supplier getSupplier() {
    return supplier;
  }

  public void setSupplier(Supplier supplier) {
    this.supplier = supplier;
  }

  /**
   * Checks if this search criteria has any filters set.
   */
  public boolean hasAnyFilter() {
    return StringUtils.isNotBlank(brandId) || StringUtils.isNotBlank(nameContains)
        || StringUtils.isNotBlank(descriptionContains) || StringUtils.isNotBlank(website) || supplier != null;
  }

  @Override
  public String toString() {
    return Json.toJson(this);
  }
}