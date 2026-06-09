package com.axonivy.utils.smart.workflow.demo.erp.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.axonivy.utils.smart.workflow.demo.erp.InMemoryBusinessDataStore;
import com.axonivy.utils.smart.workflow.demo.erp.brand.model.Brand;
import com.axonivy.utils.smart.workflow.demo.erp.brand.repository.BrandRepository;
import com.axonivy.utils.smart.workflow.demo.erp.product.category.ProductCategory;
import com.axonivy.utils.smart.workflow.demo.erp.product.category.ProductCategoryRepository;
import com.axonivy.utils.smart.workflow.demo.erp.product.model.Product;
import com.axonivy.utils.smart.workflow.demo.erp.product.repository.ProductRepository;
import com.axonivy.utils.smart.workflow.demo.erp.product.repository.ProductSearchCriteria;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.model.Supplier;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.repository.SupplierRepository;

import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
class TestProductRepository {

  private InMemoryBusinessDataStore store;
  private ProductRepository repository;
  private Supplier testSupplier;
  private ProductCategory testCategory;
  private Brand testBrand;

  @BeforeEach
  void setup() {
    store = new InMemoryBusinessDataStore();
    repository = new ProductRepository(store);

    var supplierRepo = new SupplierRepository(store);
    testSupplier = new Supplier();
    testSupplier.setBusinessName("Test Supplier");
    testSupplier = supplierRepo.create(testSupplier);

    var categoryRepo = new ProductCategoryRepository(store);
    testCategory = new ProductCategory();
    testCategory.setName("Test Category");
    testCategory = categoryRepo.create(testCategory);

    var brandRepo = new BrandRepository(store);
    testBrand = new Brand();
    testBrand.setName("Test Brand");
    testBrand = brandRepo.create(testBrand);
  }

  @Test
  void create() {
    Product created = repository.create(createProduct("Laptop", 999.99f));
    assertThat(created).isNotNull();
    assertThat(created.getProductId()).isNotBlank();
    assertThat(created.getName()).isEqualTo("Laptop");
  }

  @Test
  void createWithNullThrowsException() {
    assertThatThrownBy(() -> repository.create(null))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void findById() {
    Product created = repository.create(createProduct("Laptop", 999.99f));
    Product found = repository.findById(created.getProductId());
    assertThat(found).isNotNull();
    assertThat(found.getName()).isEqualTo("Laptop");
  }

  @Test
  void findByIdNotFound() {
    assertThat(repository.findById("non-existent")).isNull();
  }

  @Test
  void findAll() {
    repository.create(createProduct("Laptop", 999.99f));
    repository.create(createProduct("Phone", 599.99f));
    assertThat(repository.findAll()).hasSize(2);
  }

  @Test
  void update() {
    Product created = repository.create(createProduct("Laptop", 999.99f));
    Product toUpdate = repository.findById(created.getProductId());
    toUpdate.setName("Updated Laptop");
    toUpdate.setUnitPrice(1099.99f);
    toUpdate.setSupplier(testSupplier);
    toUpdate.setCategory(testCategory);
    toUpdate.setBrand(testBrand);

    Product updated = repository.update(toUpdate);
    assertThat(updated).isNotNull();
    assertThat(updated.getName()).isEqualTo("Updated Laptop");
    assertThat(updated.getUnitPrice()).isEqualTo(1099.99f);
  }

  @Test
  void updateWithNullReturnsNull() {
    assertThat(repository.update(null)).isNull();
  }

  @Test
  void delete() {
    Product created = repository.create(createProduct("Laptop", 999.99f));
    repository.delete(created);
    assertThat(repository.findById(created.getProductId())).isNull();
  }

  @Test
  void findByCriteriaByProductId() {
    Product created = repository.create(createProduct("Laptop", 999.99f));
    ProductSearchCriteria criteria = new ProductSearchCriteria();
    criteria.setProductId(created.getProductId());

    List<Product> results = repository.findByCriteria(criteria);
    assertThat(results).isNotEmpty();
  }

  @Test
  void findBySku() {
    Product product = createProduct("Laptop", 999.99f);
    product.setSku("SKU-001");
    repository.create(product);

    Product found = repository.findBySku("SKU-001");
    assertThat(found).isNotNull();
    assertThat(found.getSku()).isEqualTo("SKU-001");
  }

  @Test
  void findBySkuBlankReturnsNull() {
    assertThat(repository.findBySku("")).isNull();
  }

  @Test
  void findByCategoryIdBlankReturnsEmpty() {
    assertThat(repository.findByCategoryId("")).isEmpty();
  }

  @Test
  void findByBrandIdBlankReturnsEmpty() {
    assertThat(repository.findByBrandId("")).isEmpty();
  }

  private Product createProduct(String name, float price) {
    Product product = new Product();
    product.setName(name);
    product.setUnitPrice(price);
    product.setDescription("Test product");
    product.setActive(true);
    product.setSupplier(testSupplier);
    product.setCategory(testCategory);
    product.setBrand(testBrand);
    return product;
  }
}
