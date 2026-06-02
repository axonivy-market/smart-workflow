package com.axonivy.utils.smart.workflow.demo.erp.procurement.repository;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import ch.ivyteam.ivy.environment.Ivy;
import com.axonivy.utils.smart.workflow.demo.erp.procurement.model.ProcurementItem;

public class ProcurementItemRepository {

  private static ProcurementItemRepository instance;

  private ProcurementItemRepository() {
  }

  public static ProcurementItemRepository getInstance() {
    if (instance == null) {
      instance = new ProcurementItemRepository();
    }
    return instance;
  }

  public ProcurementItem save(ProcurementItem item) {
    if (item == null) {
      throw new IllegalArgumentException("ProcurementItem cannot be null");
    }
    Ivy.repo().save(item);
    return item;
  }

  public List<ProcurementItem> findAll() {
    return Ivy.repo().search(ProcurementItem.class).execute().getAll();
  }

  public ProcurementItem findById(String id) {
    if (StringUtils.isBlank(id)) {
      return null;
    }
    List<ProcurementItem> results = Ivy.repo().search(ProcurementItem.class)
        .textField("id").isEqualToIgnoringCase(id)
        .execute().getAll();
    return (results == null || results.isEmpty()) ? null : results.get(0);
  }

  public List<ProcurementItem> findByMaterialTypeId(String materialTypeId) {
    if (StringUtils.isBlank(materialTypeId)) {
      return new ArrayList<>();
    }
    return Ivy.repo().search(ProcurementItem.class)
        .textField("materialTypeId").isEqualToIgnoringCase(materialTypeId)
        .execute().getAll();
  }

  public List<ProcurementItem> searchByDescription(String keyword) {
    if (StringUtils.isBlank(keyword)) {
      return new ArrayList<>();
    }
    return Ivy.repo().search(ProcurementItem.class)
        .textField("materialDescription").containsAllWordPatterns(keyword)
        .execute().getAll();
  }

  public void delete(String id) {
    ProcurementItem existing = findById(id);
    if (existing != null) {
      Ivy.repo().delete(existing);
    }
  }
}
