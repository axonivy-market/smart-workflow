package com.axonivy.utils.smart.workflow.demo.supplier.onboarding.bean;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.ValidationFinding;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.enums.FindingSeverity;

import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
class TestSupplierClarificationBean {

  @Test
  void filterNonPassedFindings_excludesPassedSeverity() throws Exception {
    ValidationFinding warning = finding(FindingSeverity.WARNING);
    ValidationFinding failure = finding(FindingSeverity.FAILURE);

    List<ValidationFinding> result = invokeFilter(
        List.of(finding(FindingSeverity.PASSED), warning, failure));

    assertThat(result).containsExactly(warning, failure);
  }

  @Test
  void filterNonPassedFindings_returnsEmpty_whenNoNonPassed() throws Exception {
    assertThat(invokeFilter(null)).isEmpty();
    assertThat(invokeFilter(List.of(finding(FindingSeverity.PASSED), finding(FindingSeverity.PASSED)))).isEmpty();
  }

  @Test
  void isAllItemsResolved_whenAllResolved_returnsTrue() throws Exception {
    SupplierClarificationBean bean = new SupplierClarificationBean();
    addFinding(bean, resolved());
    addFinding(bean, resolved());

    assertThat(bean.isAllItemsResolved()).isTrue();
  }

  @Test
  void isAllItemsResolved_whenSomeUnresolved_returnsFalse() throws Exception {
    SupplierClarificationBean bean = new SupplierClarificationBean();
    addFinding(bean, resolved());
    addFinding(bean, unresolved());

    assertThat(bean.isAllItemsResolved()).isFalse();
  }

  @Test
  void isAllItemsResolved_whenEmptyList_returnsFalse() {
    assertThat(new SupplierClarificationBean().isAllItemsResolved()).isFalse();
  }

  @Test
  void markItemResolved_setsResolutionAndResetsExpandedIndex() throws Exception {
    SupplierClarificationBean bean = new SupplierClarificationBean();
    ValidationFinding f = unresolved();
    addFinding(bean, f);
    bean.toggleResolve(0);

    bean.markItemResolved(0);

    assertThat(f.getResolved()).isTrue();
    assertThat(bean.getExpandedItemIndex()).isEqualTo(-1);
  }

  @Test
  void markItemResolved_whenIndexOutOfBounds_doesNotThrow() {
    new SupplierClarificationBean().markItemResolved(99);
  }

  @Test
  void toggleResolve_expandsAndCollapsesSameIndex() {
    SupplierClarificationBean bean = new SupplierClarificationBean();
    assertThat(bean.getExpandedItemIndex()).isEqualTo(-1);

    bean.toggleResolve(2);
    assertThat(bean.getExpandedItemIndex()).isEqualTo(2);
    assertThat(bean.isItemExpanded(2)).isTrue();

    bean.toggleResolve(2);
    assertThat(bean.getExpandedItemIndex()).isEqualTo(-1);
  }

  @SuppressWarnings("unchecked")
  private static List<ValidationFinding> invokeFilter(Object input) throws Exception {
    var m = SupplierClarificationBean.class.getDeclaredMethod("filterNonPassedFindings", List.class);
    m.setAccessible(true);
    return (List<ValidationFinding>) m.invoke(null, input);
  }

  @SuppressWarnings("unchecked")
  private static void addFinding(SupplierClarificationBean bean, ValidationFinding f) throws Exception {
    var field = SupplierClarificationBean.class.getDeclaredField("clarificationFindings");
    field.setAccessible(true);
    ((List<ValidationFinding>) field.get(bean)).add(f);
  }

  private static ValidationFinding finding(FindingSeverity severity) {
    ValidationFinding f = new ValidationFinding();
    f.setSeverity(severity);
    return f;
  }

  private static ValidationFinding resolved() {
    ValidationFinding f = new ValidationFinding();
    f.setResolved(true);
    return f;
  }

  private static ValidationFinding unresolved() {
    ValidationFinding f = new ValidationFinding();
    f.setResolved(false);
    return f;
  }
}
