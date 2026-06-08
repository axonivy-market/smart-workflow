package com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.helper;

import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;

import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.ValidationFinding;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.builder.ClarificationProblemTypeBuilder;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.enums.ClarificationProblemType;

/**
 * Application-scoped bean that provides rendering helpers for {@link ValidationFinding} objects.
 * Required because d.json generated classes cannot carry custom methods.
 */
@ManagedBean(name = "validationFindingHelper")
@ApplicationScoped
public class ValidationFindingHelper {

  public ClarificationProblemType problemType(ValidationFinding finding) {
    if (finding == null) {
      return ClarificationProblemType.OTHER;
    }
    return ClarificationProblemTypeBuilder.resolve(
        finding.getDocumentTypeKey(),
        finding.getRiskKind(),
        finding.getSource(),
        finding.getMessage());
  }
}
