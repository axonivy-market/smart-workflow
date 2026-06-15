package com.axonivy.utils.smart.workflow.demo.supplier.repository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.smart.workflow.demo.AbstractMockRepository;
import com.axonivy.utils.smart.workflow.demo.document.enums.LegalDocumentType;
import com.axonivy.utils.smart.workflow.demo.mock.MockRules;
import com.axonivy.utils.smart.workflow.demo.supplier.SupplierPolicyRule;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.enums.RuleType;
import com.fasterxml.jackson.core.type.TypeReference;

public class SupplierPolicyRuleRepository extends AbstractMockRepository<SupplierPolicyRule> {

  private static final String FIELD = "MOCK_POLICY_RULES";
  private static final String ILLEGAL_ARGUMENT_MESSAGE = "SupplierPolicyRule target cannot be blank";
  private static final String NULL_ARGUMENT_MESSAGE = "SupplierPolicyRule cannot be null";
  private static final TypeReference<List<SupplierPolicyRule>> LIST_TYPE = new TypeReference<List<SupplierPolicyRule>>() {};

  private static SupplierPolicyRuleRepository instance;

  public static SupplierPolicyRuleRepository getInstance() {
    if (instance == null) {
      instance = new SupplierPolicyRuleRepository();
    }
    return instance;
  }

  @Override
  protected String getField() {
    return FIELD;
  }

  @Override
  protected TypeReference<List<SupplierPolicyRule>> getListType() {
    return LIST_TYPE;
  }

  @Override
  protected List<SupplierPolicyRule> createMockData() {
    return Arrays.stream(MockRules.values())
        .map(this::toSupplierPolicyRule)
        .collect(Collectors.toList());
  }

  public SupplierPolicyRule create(SupplierPolicyRule rule) {
    if (rule == null) {
      throw new IllegalArgumentException(NULL_ARGUMENT_MESSAGE);
    }
    if (StringUtils.isBlank(rule.getTarget())) {
      throw new IllegalArgumentException(ILLEGAL_ARGUMENT_MESSAGE);
    }
    List<SupplierPolicyRule> list = new ArrayList<>(findAll());
    list.add(rule);
    save(list);
    return rule;
  }

  public List<SupplierPolicyRule> findAllOrdered() {
    List<SupplierPolicyRule> rules = new ArrayList<>(findAll());
    rules.sort(Comparator.comparing(SupplierPolicyRule::getTarget, String.CASE_INSENSITIVE_ORDER));
    return rules;
  }

  public SupplierPolicyRule findByTarget(String target) {
    if (StringUtils.isBlank(target)) {
      return null;
    }
    return findAll().stream()
        .filter(r -> target.equalsIgnoreCase(r.getTarget()))
        .findFirst()
        .orElse(null);
  }

  private SupplierPolicyRule toSupplierPolicyRule(MockRules mockRule) {
    SupplierPolicyRule r = new SupplierPolicyRule();
    r.setTarget(mockRule.name());
    r.setRule(mockRule.rule());
    r.setRiskScore(mockRule.riskScore());
    r.setPassed(false);
    r.setRuleType(mockRule.ruleType());

    LegalDocumentType docType = mockRule.docType();
    if (docType != null && mockRule.ruleType() == RuleType.POLICY
        && docType.isCertification() && docType != LegalDocumentType.CERTIFICATION) {
      r.setCertificationType(docType);
    } else {
      r.setLegalDocumentType(docType);
    }
    return r;
  }
}
