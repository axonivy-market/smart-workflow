package com.axonivy.utils.smart.workflow.demo.erp.supplier.repository;

import java.util.Comparator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.smart.workflow.demo.erp.supplier.model.SupplierPolicyRule;

import ch.ivyteam.ivy.environment.Ivy;

public class SupplierPolicyRuleRepository {

  private static final String FIELD_TARGET = "target";

  private static SupplierPolicyRuleRepository instance;

  public static SupplierPolicyRuleRepository getInstance() {
    if (instance == null) {
      instance = new SupplierPolicyRuleRepository();
    }
    return instance;
  }

  public SupplierPolicyRule create(SupplierPolicyRule rule) {
    if (rule == null) {
      throw new IllegalArgumentException("SupplierPolicyRule cannot be null");
    }
    if (StringUtils.isBlank(rule.getTarget())) {
      throw new IllegalArgumentException("SupplierPolicyRule target cannot be blank");
    }
    Ivy.repo().save(rule);
    return rule;
  }

  public List<SupplierPolicyRule> findAll() {
    return Ivy.repo().search(SupplierPolicyRule.class).execute().getAll();
  }

  public List<SupplierPolicyRule> findAllOrdered() {
    List<SupplierPolicyRule> rules = findAll();
    rules.sort(Comparator.comparing(SupplierPolicyRule::getTarget, String.CASE_INSENSITIVE_ORDER));
    return rules;
  }

  public SupplierPolicyRule findByTarget(String target) {
    if (StringUtils.isBlank(target)) {
      return null;
    }
    return Ivy.repo().search(SupplierPolicyRule.class)
        .textField(FIELD_TARGET).isEqualToIgnoringCase(target)
        .execute().getFirst();
  }
}
