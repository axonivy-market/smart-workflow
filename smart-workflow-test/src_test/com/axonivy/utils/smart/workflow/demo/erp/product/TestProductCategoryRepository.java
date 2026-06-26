package com.axonivy.utils.smart.workflow.demo.erp.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.axonivy.utils.smart.workflow.demo.erp.InMemoryBusinessDataStore;
import com.axonivy.utils.smart.workflow.demo.erp.product.category.ProductCategory;
import com.axonivy.utils.smart.workflow.demo.erp.product.category.ProductCategoryRepository;
import com.axonivy.utils.smart.workflow.demo.erp.product.category.ProductCategorySearchCriteria;

class TestProductCategoryRepository {

  private InMemoryBusinessDataStore store;
  private ProductCategoryRepository repository;

  @BeforeEach
  void setup() {
    store = new InMemoryBusinessDataStore();
    repository = new ProductCategoryRepository(store);
  }

  @Test
  void create() {
    ProductCategory created = repository.create(createCategory("Electronics", "Electronic devices"));
    assertThat(created).isNotNull();
    assertThat(created.getCategoryId()).isNotBlank();
    assertThat(created.getName()).isEqualTo("Electronics");
  }

  @Test
  void createWithNullThrowsException() {
    assertThatThrownBy(() -> repository.create(null))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void findById() {
    ProductCategory created = repository.create(createCategory("Electronics", "Electronic devices"));
    ProductCategory found = repository.findById(created.getCategoryId());
    assertThat(found).isNotNull();
    assertThat(found.getName()).isEqualTo("Electronics");
  }

  @Test
  void findByIdNotFound() {
    assertThat(repository.findById("non-existent")).isNull();
  }

  @Test
  void findAll() {
    repository.create(createCategory("Electronics", "Electronic devices"));
    repository.create(createCategory("Clothing", "Apparel"));
    assertThat(repository.findAll()).hasSize(2);
  }

  @Test
  void update() {
    ProductCategory created = repository.create(createCategory("Electronics", "Electronic devices"));
    created.setName("Updated Electronics");

    ProductCategory updated = repository.update(created);
    assertThat(updated).isNotNull();
    assertThat(updated.getName()).isEqualTo("Updated Electronics");
  }

  @Test
  void updateWithNullReturnsNull() {
    assertThat(repository.update(null)).isNull();
  }

  @Test
  void delete() {
    ProductCategory created = repository.create(createCategory("Electronics", "Electronic devices"));
    repository.delete(created);
    assertThat(repository.findById(created.getCategoryId())).isNull();
  }

  @Test
  void findByCriteriaWithNullReturnsAll() {
    repository.create(createCategory("Electronics", "Electronic devices"));
    assertThat(repository.findByCriteria(null)).isNotEmpty();
  }

  @Test
  void findByCriteriaByCategoryId() {
    ProductCategory created = repository.create(createCategory("Electronics", "Electronic devices"));
    ProductCategorySearchCriteria criteria = new ProductCategorySearchCriteria();
    criteria.setCategoryId(created.getCategoryId());

    List<ProductCategory> results = repository.findByCriteria(criteria);
    assertThat(results).hasSize(1);
    assertThat(results.get(0).getCategoryId()).isEqualTo(created.getCategoryId());
  }

  @Test
  void findByNameContaining() {
    repository.create(createCategory("Electronics", "Electronic devices"));
    assertThat(repository.findByNameContaining("Electronics")).isNotEmpty();
  }

  @Test
  void findByNameContainingBlankReturnsEmpty() {
    assertThat(repository.findByNameContaining("")).isEmpty();
  }

  @Test
  void findExactProductCategoryWithNullReturnsNull() {
    assertThat(repository.findExactProductCategory(null)).isNull();
  }

  @Test
  void findExactProductCategoryByName() {
    repository.create(createCategory("Electronics", "Electronic devices"));
    ProductCategorySearchCriteria criteria = new ProductCategorySearchCriteria();
    criteria.setNameContains("Electronics");

    ProductCategory found = repository.findExactProductCategory(criteria);
    assertThat(found).isNotNull();
    assertThat(found.getName()).isEqualTo("Electronics");
  }

  private ProductCategory createCategory(String name, String description) {
    ProductCategory category = new ProductCategory();
    category.setName(name);
    category.setDescription(description);
    return category;
  }
}
