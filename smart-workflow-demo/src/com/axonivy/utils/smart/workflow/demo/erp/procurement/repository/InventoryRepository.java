package com.axonivy.utils.smart.workflow.demo.erp.procurement.repository;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import ch.ivyteam.ivy.environment.Ivy;
import com.axonivy.utils.smart.workflow.demo.erp.procurement.model.InventoryItem;
import com.axonivy.utils.smart.workflow.demo.erp.procurement.model.ProcurementItem;

public class InventoryRepository {

  private static InventoryRepository instance;

  private InventoryRepository() {
  }

  public static InventoryRepository getInstance() {
    if (instance == null) {
      instance = new InventoryRepository();
    }
    return instance;
  }

  public InventoryItem save(InventoryItem item) {
    if (item == null) {
      throw new IllegalArgumentException("InventoryItem cannot be null");
    }
    Ivy.repo().save(item);
    return item;
  }

  public List<InventoryItem> findAll() {
    List<InventoryItem> results = Ivy.repo().search(InventoryItem.class).execute().getAll();
    results.forEach(this::enrich);
    return results;
  }

  public InventoryItem findById(String id) {
    if (StringUtils.isBlank(id)) {
      return null;
    }
    List<InventoryItem> results = Ivy.repo().search(InventoryItem.class)
        .textField("id").isEqualToIgnoringCase(id)
        .execute().getAll();
    InventoryItem item = (results == null || results.isEmpty()) ? null : results.get(0);
    enrich(item);
    return item;
  }

  public InventoryItem findByMaterialTypeId(String materialTypeId) {
    if (StringUtils.isBlank(materialTypeId)) {
      return null;
    }
    List<InventoryItem> results = Ivy.repo().search(InventoryItem.class)
        .textField("materialTypeId").isEqualToIgnoringCase(materialTypeId)
        .execute().getAll();
    InventoryItem item = (results == null || results.isEmpty()) ? null : results.get(0);
    enrich(item);
    return item;
  }

  public InventoryItem findByMaterialItemId(String materialItemId) {
    if (StringUtils.isBlank(materialItemId)) {
      return null;
    }
    List<InventoryItem> results = Ivy.repo().search(InventoryItem.class)
        .textField("materialItemId").isEqualToIgnoringCase(materialItemId)
        .execute().getAll();
    InventoryItem item = (results == null || results.isEmpty()) ? null : results.get(0);
    enrich(item);
    return item;
  }

  public List<InventoryItem> findSimilarItems(ProcurementItem materialItem) {
    if (materialItem == null || StringUtils.isBlank(materialItem.getMaterialTypeId())) {
      return new ArrayList<>();
    }
    List<InventoryItem> results = findAllByMaterialTypeId(materialItem.getMaterialTypeId());
    results.removeIf(inv -> materialItem.getMaterialDescription() != null && materialItem.getMaterialDescription().equals(inv.getMaterialItem().getMaterialDescription()));
    return results;
  }

  public List<InventoryItem> findAllByMaterialTypeId(String materialTypeId) {
    if (StringUtils.isBlank(materialTypeId)) {
      return new ArrayList<>();
    }
    List<InventoryItem> results = Ivy.repo().search(InventoryItem.class)
        .textField("materialTypeId").isEqualToIgnoringCase(materialTypeId)
        .execute().getAll();
    results.forEach(this::enrich);
    return results;
  }

  public List<InventoryItem> searchByMaterialTypeName(String keyword) {
    if (StringUtils.isBlank(keyword)) {
      return new ArrayList<>();
    }
    List<InventoryItem> results = Ivy.repo().search(InventoryItem.class)
        .textField("materialTypeName").containsAllWordPatterns(keyword)
        .execute().getAll();
    results.forEach(this::enrich);
    return results;
  }

  public List<InventoryItem> findByLocation(String location) {
    if (StringUtils.isBlank(location)) {
      return new ArrayList<>();
    }
    List<InventoryItem> results = Ivy.repo().search(InventoryItem.class)
        .textField("location").isEqualToIgnoringCase(location)
        .execute().getAll();
    results.forEach(this::enrich);
    return results;
  }

  public void delete(String id) {
    InventoryItem existing = findById(id);
    if (existing != null) {
      Ivy.repo().delete(existing);
    }
  }

  private void enrich(InventoryItem item) {
    if (item == null || StringUtils.isBlank(item.getMaterialItemId())) {
      return;
    }
    item.setMaterialItem(ProcurementItemRepository.getInstance().findById(item.getMaterialItemId()));
  }
}
