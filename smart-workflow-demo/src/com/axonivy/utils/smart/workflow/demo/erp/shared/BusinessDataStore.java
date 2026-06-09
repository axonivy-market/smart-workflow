package com.axonivy.utils.smart.workflow.demo.erp.shared;

import java.util.List;

public interface BusinessDataStore {

  <T> void save(T entity);

  <T> void delete(T entity);

  <T> List<T> findAll(Class<T> type);

  <T> T findFirstByField(Class<T> type, String fieldName, String value);

  <T> List<T> findByField(Class<T> type, String fieldName, String value);

  <T> List<T> findByFieldContaining(Class<T> type, String fieldName, String pattern);
}
