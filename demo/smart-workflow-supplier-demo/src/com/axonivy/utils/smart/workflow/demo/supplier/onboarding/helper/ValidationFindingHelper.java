package com.axonivy.utils.smart.workflow.demo.supplier.onboarding.helper;

import java.util.Optional;

import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;

import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.ValidationFinding;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.builder.ClarificationProblemTypeBuilder;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.enums.ClarificationProblemType;

@ManagedBean(name = "validationFindingHelper")
@ApplicationScoped
public class ValidationFindingHelper {

  public ClarificationProblemType problemType(ValidationFinding finding) {
    return Optional.ofNullable(finding)
        .map(ClarificationProblemTypeBuilder::resolve)
        .orElse(ClarificationProblemType.OTHER);
  }
}
