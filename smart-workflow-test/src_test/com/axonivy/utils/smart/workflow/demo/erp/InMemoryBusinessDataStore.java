package com.axonivy.utils.smart.workflow.demo.erp;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.axonivy.utils.smart.workflow.demo.erp.shared.BusinessDataStore;

public class InMemoryBusinessDataStore implements BusinessDataStore {

  private final List<Object> entities = new ArrayList<>();

  @Override
  public <T> void save(T entity) {
    if (!entities.contains(entity)) {
      entities.add(entity);
    }
  }

  @Override
  public <T> void delete(T entity) {
    entities.remove(entity);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> List<T> findAll(Class<T> type) {
    return entities.stream()
        .filter(type::isInstance)
        .map(e -> (T) e)
        .collect(Collectors.toList());
  }

  @Override
  public <T> T findFirstByField(Class<T> type, String fieldName, String value) {
    List<T> results = findByField(type, fieldName, value);
    return results.isEmpty() ? null : results.get(0);
  }

  @Override
  public <T> List<T> findByField(Class<T> type, String fieldName, String value) {
    return findAll(type).stream()
        .filter(entity -> matchesField(entity, fieldName, value))
        .collect(Collectors.toList());
  }

  @Override
  public <T> List<T> findByFieldContaining(Class<T> type, String fieldName, String pattern) {
    return findAll(type).stream()
        .filter(entity -> fieldContains(entity, fieldName, pattern))
        .collect(Collectors.toList());
  }

  private boolean matchesField(Object entity, String fieldName, String value) {
    String fieldValue = getFieldValue(entity, fieldName);
    if (fieldValue == null || value == null) {
      return fieldValue == null && value == null;
    }
    return fieldValue.equalsIgnoreCase(value);
  }

  private boolean fieldContains(Object entity, String fieldName, String pattern) {
    String fieldValue = getFieldValue(entity, fieldName);
    if (fieldValue == null || pattern == null) {
      return false;
    }
    return fieldValue.toLowerCase().contains(pattern.toLowerCase());
  }

  private String getFieldValue(Object entity, String fieldName) {
    try {
      String getterName = "get" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
      Method getter = entity.getClass().getMethod(getterName);
      Object value = getter.invoke(entity);
      if (value == null) {
        return null;
      }
      if (value instanceof Boolean) {
        getterName = "is" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
        try {
          getter = entity.getClass().getMethod(getterName);
          value = getter.invoke(entity);
        } catch (NoSuchMethodException e) {
          // use the get version
        }
      }
      return String.valueOf(value);
    } catch (NoSuchMethodException e) {
      try {
        String isGetterName = "is" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
        Method getter = entity.getClass().getMethod(isGetterName);
        Object value = getter.invoke(entity);
        return value == null ? null : String.valueOf(value);
      } catch (Exception ex) {
        return null;
      }
    } catch (Exception e) {
      return null;
    }
  }

  public void clear() {
    entities.clear();
  }
}
