package com.axonivy.utils.smart.workflow.demo.shopping.common;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;

import ch.ivyteam.ivy.environment.Ivy;

public class KeywordRepository {
  private static KeywordRepository instance;

  public static KeywordRepository getInstance() {
    if (instance == null) {
      instance = new KeywordRepository();
    }
    return instance;
  }
  
  public KeywordPool findAll() {
    return Ivy.repo().search(KeywordPool.class).execute().getAll().getFirst();
  }

  public KeywordPool update(List<String> newKeywords) {
    KeywordPool current = findAll();
    if (CollectionUtils.isEmpty(newKeywords)) {
      return current;
    }

    current.getKeywords().addAll(newKeywords);
    List<String> updated = current.getKeywords().stream().distinct().collect(Collectors.toList());
    current.setKeywords(updated);
    Ivy.repo().save(current);

    return current;
  }

  public KeywordPool save(KeywordPool keywordPool) {
    KeywordPool current = findAll();
    if (CollectionUtils.isEmpty(Optional.ofNullable(keywordPool).map(KeywordPool::getKeywords).orElse(null))) {
      return current;
    }

    current.setKeywords(keywordPool.getKeywords());
    Ivy.repo().save(current);
    return current;
  }
}
