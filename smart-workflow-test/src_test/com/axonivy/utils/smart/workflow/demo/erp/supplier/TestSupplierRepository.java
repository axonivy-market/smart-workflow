package com.axonivy.utils.smart.workflow.demo.erp.supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.axonivy.utils.smart.workflow.demo.erp.InMemoryBusinessDataStore;
import com.axonivy.utils.smart.workflow.demo.erp.shared.Address;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.model.Supplier;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.repository.SupplierRepository;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.repository.SupplierSearchCriteria;

import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
class TestSupplierRepository {

  private InMemoryBusinessDataStore store;
  private SupplierRepository repository;

  @BeforeEach
  void setup() {
    store = new InMemoryBusinessDataStore();
    repository = new SupplierRepository(store);
  }

  @Test
  void create() {
    Supplier created = repository.create(createSupplier("Acme Corp", "acme@test.com"));
    assertThat(created).isNotNull();
    assertThat(created.getSupplierId()).isNotBlank();
    assertThat(created.getBusinessName()).isEqualTo("Acme Corp");
  }

  @Test
  void createWithNullThrowsException() {
    assertThatThrownBy(() -> repository.create(null))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void findById() {
    Supplier created = repository.create(createSupplier("Acme Corp", "acme@test.com"));
    Supplier found = repository.findById(created.getSupplierId());
    assertThat(found).isNotNull();
    assertThat(found.getBusinessName()).isEqualTo("Acme Corp");
  }

  @Test
  void findByIdNotFound() {
    assertThat(repository.findById("non-existent")).isNull();
  }

  @Test
  void findAll() {
    repository.create(createSupplier("Acme Corp", "acme@test.com"));
    repository.create(createSupplier("Global Inc", "global@test.com"));
    assertThat(repository.findAll()).hasSize(2);
  }

  @Test
  void update() {
    Supplier created = repository.create(createSupplier("Acme Corp", "acme@test.com"));
    created.setBusinessName("Updated Acme");
    created.setEmail("updated@test.com");

    Supplier updated = repository.update(created);
    assertThat(updated).isNotNull();
    assertThat(updated.getBusinessName()).isEqualTo("Updated Acme");
    assertThat(updated.getEmail()).isEqualTo("updated@test.com");
  }

  @Test
  void updateWithNullReturnsNull() {
    assertThat(repository.update(null)).isNull();
  }

  @Test
  void delete() {
    Supplier created = repository.create(createSupplier("Acme Corp", "acme@test.com"));
    repository.delete(created);
    assertThat(repository.findById(created.getSupplierId())).isNull();
  }

  @Test
  void deleteWithNullDoesNotThrow() {
    repository.delete(null);
  }

  @Test
  void findByCriteriaWithNullReturnsAll() {
    repository.create(createSupplier("Acme Corp", "acme@test.com"));
    assertThat(repository.findByCriteria(null)).isNotEmpty();
  }

  @Test
  void findByCriteriaBySupplierId() {
    Supplier created = repository.create(createSupplier("Acme Corp", "acme@test.com"));
    SupplierSearchCriteria criteria = new SupplierSearchCriteria();
    criteria.setSupplierId(created.getSupplierId());

    List<Supplier> results = repository.findByCriteria(criteria);
    assertThat(results).hasSize(1);
    assertThat(results.get(0).getSupplierId()).isEqualTo(created.getSupplierId());
  }

  @Test
  void findByCriteriaByEmail() {
    repository.create(createSupplier("Acme Corp", "acme@test.com"));
    SupplierSearchCriteria criteria = new SupplierSearchCriteria();
    criteria.setEmail("acme@test.com");

    assertThat(repository.findByCriteria(criteria)).isNotEmpty();
  }

  @Test
  void findByCriteriaByPhone() {
    Supplier supplier = createSupplier("Acme Corp", "acme@test.com");
    supplier.setPhone("+1234567890");
    repository.create(supplier);

    SupplierSearchCriteria criteria = new SupplierSearchCriteria();
    criteria.setPhone("+1234567890");

    assertThat(repository.findByCriteria(criteria)).isNotEmpty();
  }

  @Test
  void findByCriteriaByBusinessName() {
    repository.create(createSupplier("Acme Corp", "acme@test.com"));
    SupplierSearchCriteria criteria = new SupplierSearchCriteria();
    criteria.setBusinessNameContains("Acme");

    assertThat(repository.findByCriteria(criteria)).isNotEmpty();
  }

  @Test
  void findExactSupplierWithNullReturnsNull() {
    assertThat(repository.findExactSupplier(null)).isNull();
  }

  @Test
  void findExactSupplierByName() {
    repository.create(createSupplier("Acme Corp", "acme@test.com"));
    SupplierSearchCriteria criteria = new SupplierSearchCriteria();
    criteria.setBusinessNameContains("Acme Corp");

    Supplier found = repository.findExactSupplier(criteria);
    assertThat(found).isNotNull();
    assertThat(found.getBusinessName()).isEqualTo("Acme Corp");
  }

  private Supplier createSupplier(String businessName, String email) {
    Supplier supplier = new Supplier();
    supplier.setBusinessName(businessName);
    supplier.setEmail(email);
    supplier.setWebsite("https://example.com");
    supplier.setBusinessAddress(new Address("123 Main St", "", "City", "State", "12345", "US"));
    return supplier;
  }
}
