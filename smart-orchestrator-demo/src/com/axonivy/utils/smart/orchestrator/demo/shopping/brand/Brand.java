package com.axonivy.utils.smart.orchestrator.demo.shopping.brand;

import com.fasterxml.jackson.annotation.JsonIgnore;

import dev.langchain4j.model.output.structured.Description;

public class Brand {

  @Description("Unique brand identifier")
  private String brandId;

  @Description("Name of the brand")
  private String name;

  @Description("Detailed description of the brand")
  private String description;

  @Description("Logo image encoded in Base64 format")
  private String logoBase64;

  @Description("Official website URL of the brand")
  private String website;

  @JsonIgnore
  private Double matchingScore;

  public String getBrandId() {
    return brandId;
  }

  public void setBrandId(String brandId) {
    this.brandId = brandId;
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

  public String getLogoBase64() {
    return logoBase64;
  }

  public void setLogoBase64(String logoBase64) {
    this.logoBase64 = logoBase64;
  }

  public String getWebsite() {
    return website;
  }

  public void setWebsite(String website) {
    this.website = website;
  }

  public Double getMatchingScore() {
    return matchingScore;
  }

  public void setMatchingScore(Double matchingScore) {
    this.matchingScore = matchingScore;
  }
}