package com.axonivy.utils.smart.workflow.guardrails.internal.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface SmartWorkflowGuardrail {

  GuardrailType type() default GuardrailType.INPUT;

  enum GuardrailType {
    INPUT, OUTPUT;
  }
}
