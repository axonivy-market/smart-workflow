package com.axonivy.utils.smart.workflow.demo.supplier.onboarding.helper;

import java.io.Serializable;
import java.util.Optional;

import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.ValidationFinding;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.builder.ClarificationProblemTypeBuilder;
import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.enums.ClarificationProblemType;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;

@Named(value = "validationFindingHelper")
@ApplicationScoped
public class ValidationFindingHelper implements  Serializable {

  private static final long serialVersionUID = 1L;

  public ClarificationProblemType problemType(ValidationFinding finding) {
    return Optional.ofNullable(finding)
        .map(ClarificationProblemTypeBuilder::resolve)
        .orElse(ClarificationProblemType.OTHER);
  }
}
