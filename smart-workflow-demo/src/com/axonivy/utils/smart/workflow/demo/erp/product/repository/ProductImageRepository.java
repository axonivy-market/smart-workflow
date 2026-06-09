package com.axonivy.utils.smart.workflow.demo.erp.product.repository;

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.smart.workflow.demo.erp.product.model.ProductImage;
import com.axonivy.utils.smart.workflow.demo.erp.shared.BusinessDataStore;
import com.axonivy.utils.smart.workflow.demo.erp.shared.IvyBusinessDataStore;
import com.axonivy.utils.smart.workflow.utils.IdGenerationUtils;

public class ProductImageRepository {

  private static ProductImageRepository instance;

  private final BusinessDataStore store;

  public ProductImageRepository() {
    this(IvyBusinessDataStore.getInstance());
  }

  public ProductImageRepository(BusinessDataStore store) {
    this.store = store;
  }

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

    store.save(image);
    return image.getImageId();
  }

  public ProductImage findById(String id) {
    return store.findFirstByField(ProductImage.class, "imageId", id);
  }
}
