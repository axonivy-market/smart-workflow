package com.axonivy.utils.smart.workflow.demo.erp.brand;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.axonivy.utils.smart.workflow.demo.erp.InMemoryBusinessDataStore;
import com.axonivy.utils.smart.workflow.demo.erp.brand.model.Brand;
import com.axonivy.utils.smart.workflow.demo.erp.brand.repository.BrandRepository;
import com.axonivy.utils.smart.workflow.demo.erp.brand.repository.BrandSearchCriteria;

class TestBrandRepository {

  private InMemoryBusinessDataStore store;
  private BrandRepository repository;

  @BeforeEach
  void setup() {
    store = new InMemoryBusinessDataStore();
    repository = new BrandRepository(store);
  }

  @Test
  void create() {
    Brand created = repository.create(createBrand("Test Brand", "A test brand", "https://test.com"));
    assertThat(created).isNotNull();
    assertThat(created.getBrandId()).isNotBlank();
    assertThat(created.getName()).isEqualTo("Test Brand");
  }

  @Test
  void createWithNullThrowsException() {
    assertThatThrownBy(() -> repository.create(null))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void findById() {
    Brand created = repository.create(createBrand("Test Brand", "A test brand", "https://test.com"));
    Brand found = repository.findById(created.getBrandId());
    assertThat(found).isNotNull();
    assertThat(found.getName()).isEqualTo("Test Brand");
  }

  @Test
  void findByIdNotFound() {
    assertThat(repository.findById("non-existent")).isNull();
  }

  @Test
  void findAll() {
    repository.create(createBrand("Brand 1", "Desc 1", "https://b1.com"));
    repository.create(createBrand("Brand 2", "Desc 2", "https://b2.com"));
    assertThat(repository.findAll()).hasSize(2);
  }

  @Test
  void update() {
    Brand created = repository.create(createBrand("Test Brand", "Desc", "https://test.com"));
    created.setName("Updated Brand");
    created.setDescription("Updated description");

    Brand updated = repository.update(created);
    assertThat(updated).isNotNull();
    assertThat(updated.getName()).isEqualTo("Updated Brand");
    assertThat(updated.getDescription()).isEqualTo("Updated description");
  }

  @Test
  void updateWithNullReturnsNull() {
    assertThat(repository.update(null)).isNull();
  }

  @Test
  void delete() {
    Brand created = repository.create(createBrand("Test Brand", "Desc", "https://test.com"));
    repository.delete(created);
    assertThat(repository.findById(created.getBrandId())).isNull();
  }

  @Test
  void deleteWithNullDoesNotThrow() {
    repository.delete(null);
  }

  @Test
  void findByCriteriaWithNullReturnsAll() {
    repository.create(createBrand("Brand", "Desc", "https://test.com"));
    assertThat(repository.findByCriteria(null)).isNotEmpty();
  }

  @Test
  void findByCriteriaByBrandId() {
    Brand created = repository.create(createBrand("Test Brand", "Desc", "https://test.com"));
    BrandSearchCriteria criteria = new BrandSearchCriteria();
    criteria.setBrandId(created.getBrandId());

    List<Brand> results = repository.findByCriteria(criteria);
    assertThat(results).hasSize(1);
    assertThat(results.get(0).getBrandId()).isEqualTo(created.getBrandId());
  }

  @Test
  void findByCriteriaByWebsite() {
    repository.create(createBrand("Test Brand", "Desc", "https://test.com"));
    BrandSearchCriteria criteria = new BrandSearchCriteria();
    criteria.setWebsite("https://test.com");

    assertThat(repository.findByCriteria(criteria)).isNotEmpty();
  }

  @Test
  void findByCriteriaByName() {
    repository.create(createBrand("Test Brand", "Desc", "https://test.com"));
    BrandSearchCriteria criteria = new BrandSearchCriteria();
    criteria.setNameContains("Test");

    assertThat(repository.findByCriteria(criteria)).isNotEmpty();
  }

  @Test
  void findExactBrandWithNullReturnsNull() {
    assertThat(repository.findExactBrand(null)).isNull();
  }

  @Test
  void findExactBrandByName() {
    repository.create(createBrand("Test Brand", "Desc", "https://test.com"));
    BrandSearchCriteria criteria = new BrandSearchCriteria();
    criteria.setNameContains("Test Brand");

    Brand found = repository.findExactBrand(criteria);
    assertThat(found).isNotNull();
    assertThat(found.getName()).isEqualTo("Test Brand");
  }

  private Brand createBrand(String name, String description, String website) {
    Brand brand = new Brand();
    brand.setName(name);
    brand.setDescription(description);
    brand.setWebsite(website);
    return brand;
  }
}
