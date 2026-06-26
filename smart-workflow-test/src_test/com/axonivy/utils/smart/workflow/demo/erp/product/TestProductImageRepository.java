package com.axonivy.utils.smart.workflow.demo.erp.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.axonivy.utils.smart.workflow.demo.erp.InMemoryBusinessDataStore;
import com.axonivy.utils.smart.workflow.demo.erp.product.model.ProductImage;
import com.axonivy.utils.smart.workflow.demo.erp.product.repository.ProductImageRepository;

import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
class TestProductImageRepository {

  private InMemoryBusinessDataStore store;
  private ProductImageRepository repository;

  @BeforeEach
  void setup() {
    store = new InMemoryBusinessDataStore();
    repository = new ProductImageRepository(store);
  }

  @Test
  void create() {
    ProductImage image = new ProductImage();
    image.setContent("base64Content");

    String imageId = repository.create(image);
    assertThat(imageId).isNotBlank();
  }

  @Test
  void createWithNullContentThrowsException() {
    ProductImage image = new ProductImage();
    assertThatThrownBy(() -> repository.create(image))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void createWithNullThrowsException() {
    assertThatThrownBy(() -> repository.create(null))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void findById() {
    ProductImage image = new ProductImage();
    image.setContent("base64Content");

    String imageId = repository.create(image);
    ProductImage found = repository.findById(imageId);
    assertThat(found).isNotNull();
    assertThat(found.getContent()).isEqualTo("base64Content");
  }

  @Test
  void findByIdNotFound() {
    assertThat(repository.findById("non-existent")).isNull();
  }
}
