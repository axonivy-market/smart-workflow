package com.axonivy.utils.smart.orchestrator.demo.shopping.product.image;

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.smart.orchestrator.utils.IdGenerationUtils;

import ch.ivyteam.ivy.environment.Ivy;

public class ProductImageRepository {

  private static ProductImageRepository instance;

  public static ProductImageRepository getInstance() {
    if (instance == null) {
      instance = new ProductImageRepository();
    }
    return instance;
  }

  public String create(ProductImage image) {
    if (StringUtils.isBlank(Optional.ofNullable(image).map(ProductImage::getContent).orElse(StringUtils.EMPTY))) {
      throw new IllegalArgumentException("Product cannot be null");
    }

    if (StringUtils.isBlank(image.getImageId())) {
      image.setImageId(IdGenerationUtils.generateRandomId());
    }

    Ivy.repo().save(image);
    return image.getImageId();
  }

  public ProductImage findById(String id) {
    return Ivy.repo().search(ProductImage.class).textField("imageId").isEqualToIgnoringCase(id).execute().getFirst();
  }
}