package com.axonivy.utils.smart.workflow.demo.supplier.onboarding.bean;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.axonivy.utils.smart.workflow.demo.assistant.AgentGuidance;
import com.axonivy.utils.smart.workflow.demo.enums.Status;
import com.axonivy.utils.smart.workflow.demo.supplier.Supplier;
import com.axonivy.utils.smart.workflow.demo.supplier.agent.SupplierAgentResponse;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.OnboardingRequest;

import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
class TestSupplierDuplicateCheckBean {

  @Test
  void init_setsFieldsAndIsIdempotent() {
    SupplierDuplicateCheckBean bean = new SupplierDuplicateCheckBean();
    OnboardingRequest request = new OnboardingRequest();
    SupplierAgentResponse response = new SupplierAgentResponse();

    bean.init(request, response);

    assertThat(bean.getRequest()).isSameAs(request);
    assertThat(bean.getAgentResponse()).isSameAs(response);

    OnboardingRequest other = new OnboardingRequest();
    bean.init(other, new SupplierAgentResponse());

    assertThat(bean.getRequest()).isSameAs(request);
  }

  @Test
  void isHasMatches_returnsFalse_whenNoMatchedSuppliers() {
    SupplierDuplicateCheckBean bean = new SupplierDuplicateCheckBean();
    bean.init(new OnboardingRequest(), null);

    assertThat(bean.isHasMatches()).isFalse();
  }

  @Test
  void isHasMatches_returnsTrue_whenMatchedSuppliersPresent() {
    SupplierDuplicateCheckBean bean = new SupplierDuplicateCheckBean();
    OnboardingRequest request = new OnboardingRequest();
    request.setMatchedSuppliers(List.of(new Supplier()));
    bean.init(request, null);

    assertThat(bean.isHasMatches()).isTrue();
  }

  @Test
  void getMatches_returnsEmptyList_whenMatchedSuppliersIsNull() {
    SupplierDuplicateCheckBean bean = new SupplierDuplicateCheckBean();
    bean.init(new OnboardingRequest(), null);

    assertThat(bean.getMatches()).isEmpty();
  }

  @Test
  void getMatches_returnsList_whenMatchedSuppliersSet() {
    SupplierDuplicateCheckBean bean = new SupplierDuplicateCheckBean();
    Supplier s1 = new Supplier();
    s1.setBusinessName("ACME Corp");
    Supplier s2 = new Supplier();
    s2.setBusinessName("Beta GmbH");
    OnboardingRequest request = new OnboardingRequest();
    request.setMatchedSuppliers(List.of(s1, s2));
    bean.init(request, null);

    assertThat(bean.getMatches()).containsExactly(s1, s2);
  }

  @Test
  void getMatchCount_returnsZero_whenNoMatches() {
    SupplierDuplicateCheckBean bean = new SupplierDuplicateCheckBean();
    bean.init(new OnboardingRequest(), null);

    assertThat(bean.getMatchCount()).isZero();
  }

  @Test
  void getMatchCount_returnsCorrectCount() {
    SupplierDuplicateCheckBean bean = new SupplierDuplicateCheckBean();
    OnboardingRequest request = new OnboardingRequest();
    request.setMatchedSuppliers(List.of(new Supplier(), new Supplier(), new Supplier()));
    bean.init(request, null);

    assertThat(bean.getMatchCount()).isEqualTo(3);
  }

  @Test
  void getColorClass_returnsErrorColor_whenStatusIsError() {
    SupplierDuplicateCheckBean bean = new SupplierDuplicateCheckBean();
    SupplierAgentResponse response = new SupplierAgentResponse();
    response.setStatus(Status.ERROR);
    bean.init(new OnboardingRequest(), response);

    assertThat(bean.getColorClass()).isEqualTo(Status.ERROR.colorClass);
  }

  @Test
  void getColorClass_returnsYellow_whenHasMatches() {
    SupplierDuplicateCheckBean bean = new SupplierDuplicateCheckBean();
    OnboardingRequest request = new OnboardingRequest();
    request.setMatchedSuppliers(List.of(new Supplier()));
    bean.init(request, new SupplierAgentResponse());

    assertThat(bean.getColorClass()).isEqualTo("text-yellow-600");
  }

  @Test
  void getColorClass_returnsSuccessColor_whenNoMatchesAndNoError() {
    SupplierDuplicateCheckBean bean = new SupplierDuplicateCheckBean();
    bean.init(new OnboardingRequest(), new SupplierAgentResponse());

    assertThat(bean.getColorClass()).isEqualTo(Status.SUCCESS.colorClass);
  }

  @Test
  void getIconClass_returnsErrorIcon_whenStatusIsError() {
    SupplierDuplicateCheckBean bean = new SupplierDuplicateCheckBean();
    SupplierAgentResponse response = new SupplierAgentResponse();
    response.setStatus(Status.ERROR);
    bean.init(new OnboardingRequest(), response);

    assertThat(bean.getIconClass()).isEqualTo(Status.ERROR.iconClass);
  }

  @Test
  void getIconClass_returnsWarningIcon_whenHasMatches() {
    SupplierDuplicateCheckBean bean = new SupplierDuplicateCheckBean();
    OnboardingRequest request = new OnboardingRequest();
    request.setMatchedSuppliers(List.of(new Supplier()));
    bean.init(request, new SupplierAgentResponse());

    assertThat(bean.getIconClass()).isEqualTo("ti-alert-triangle");
  }

  @Test
  void getIconClass_returnsSuccessIcon_whenNoMatchesAndNoError() {
    SupplierDuplicateCheckBean bean = new SupplierDuplicateCheckBean();
    bean.init(new OnboardingRequest(), new SupplierAgentResponse());

    assertThat(bean.getIconClass()).isEqualTo(Status.SUCCESS.iconClass);
  }

  @Test
  void getAgentGuidance_returns4EntriesWithNonBlankContent() {
    SupplierDuplicateCheckBean bean = new SupplierDuplicateCheckBean();

    List<AgentGuidance> guidance = bean.getAgentGuidance();

    assertThat(guidance).hasSize(4);
    assertThat(guidance).allSatisfy(g -> {
      assertThat(g.getQuestionPattern()).isNotBlank();
      assertThat(g.getInstruction()).isNotBlank();
    });
  }
}
