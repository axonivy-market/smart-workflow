package com.axonivy.utils.smart.workflow.demo.erp.shared;

import java.util.List;

import ch.ivyteam.ivy.environment.Ivy;

public class IvyBusinessDataStore implements BusinessDataStore {

  private static final IvyBusinessDataStore INSTANCE = new IvyBusinessDataStore();

  public static IvyBusinessDataStore getInstance() {
    return INSTANCE;
  }

  @Override
  public <T> void save(T entity) {
    Ivy.repo().save(entity);
  }

  @Override
  public <T> void delete(T entity) {
    Ivy.repo().delete(entity);
  }

  @Override
  public <T> List<T> findAll(Class<T> type) {
    return Ivy.repo().search(type).execute().getAll();
  }

  @Override
  public <T> T findFirstByField(Class<T> type, String fieldName, String value) {
    return Ivy.repo().search(type).textField(fieldName).isEqualToIgnoringCase(value).execute().getFirst();
  }

  @Override
  public <T> List<T> findByField(Class<T> type, String fieldName, String value) {
    return Ivy.repo().search(type).textField(fieldName).isEqualToIgnoringCase(value).execute().getAll();
  }

  @Override
  public <T> List<T> findByFieldContaining(Class<T> type, String fieldName, String pattern) {
    return Ivy.repo().search(type).textField(fieldName).containsAllWordPatterns(pattern).execute().getAll();
  }
}
