package com.axonivy.utils.smart.workflow.demo.shopping.searchkeyword;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ch.ivyteam.ivy.environment.Ivy;

public class ProductKeywordRepository {
  private static ProductKeywordRepository instance;

  public static ProductKeywordRepository getInstance() {
    if (instance == null) {
      instance = new ProductKeywordRepository();
    }
    return instance;
  }

  /**
   * Retrieves all products.
   *
   * @return list of all products
   */
  public Set<String> extractKeywords() {
    List<ProductKeywords> existingKeyWords = Ivy.repo().search(ProductKeywords.class).execute().getAll();
    Set<String> result = new HashSet<>();
    existingKeyWords.forEach(keyword -> result.addAll(new HashSet<>(keyword.getKeywords())));
    return result;
  }

  public Set<String> findProductIdByKeyWords(Set<String> keywords) {
    Ivy.log().warn(keywords);
    List<ProductKeywords> existingKeyWords = Ivy.repo().search(ProductKeywords.class).textField("keywords")
        .containsAnyWords(keywords.toArray(new String[0])).execute().getAll();
    Set<String> result = new HashSet<>();
    existingKeyWords.forEach(keyword -> result.add(keyword.getProductId()));
    return result;
  }
}
