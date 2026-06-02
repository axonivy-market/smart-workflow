package com.axonivy.utils.smart.workflow.demo.erp.procurement.repository;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import ch.ivyteam.ivy.environment.Ivy;
import com.axonivy.utils.smart.workflow.demo.erp.procurement.model.MaterialType;

public class MaterialTypeRepository {

  private static MaterialTypeRepository instance;

  private MaterialTypeRepository() {
  }

  public static MaterialTypeRepository getInstance() {
    if (instance == null) {
      instance = new MaterialTypeRepository();
    }
    return instance;
  }

  public MaterialType save(MaterialType materialType) {
    if (materialType == null) {
      throw new IllegalArgumentException("MaterialType cannot be null");
    }
    Ivy.repo().save(materialType);
    return materialType;
  }

  public List<MaterialType> findAll() {
    return Ivy.repo().search(MaterialType.class).execute().getAll();
  }

  public List<MaterialType> findAllActive() {
    return Ivy.repo().search(MaterialType.class)
        .textField("active").isEqualToIgnoringCase("true")
        .execute().getAll();
  }

  public MaterialType findById(String id) {
    if (StringUtils.isBlank(id)) {
      return null;
    }
    List<MaterialType> results = Ivy.repo().search(MaterialType.class)
        .textField("id").isEqualToIgnoringCase(id)
        .execute().getAll();
    return (results == null || results.isEmpty()) ? null : results.get(0);
  }

  public MaterialType findByName(String name) {
    if (StringUtils.isBlank(name)) {
      return null;
    }
    List<MaterialType> results = Ivy.repo().search(MaterialType.class)
        .textField("name").isEqualToIgnoringCase(name)
        .execute().getAll();
    return (results == null || results.isEmpty()) ? null : results.get(0);
  }

  public List<MaterialType> searchByName(String keyword) {
    if (StringUtils.isBlank(keyword)) {
      return new ArrayList<>();
    }
    return Ivy.repo().search(MaterialType.class)
        .textField("name").containsAllWordPatterns(keyword)
        .execute().getAll();
  }

  public void delete(String id) {
    MaterialType existing = findById(id);
    if (existing != null) {
      existing.setActive(false);
      Ivy.repo().save(existing);
    }
  }
}
