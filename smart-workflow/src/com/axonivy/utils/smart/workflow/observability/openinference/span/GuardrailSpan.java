package com.axonivy.utils.smart.workflow.observability.openinference.span;

import java.util.List;
import java.util.function.Supplier;

import ch.ivyteam.ivy.trace.Attribute;
import ch.ivyteam.ivy.trace.SpanInstance;
import ch.ivyteam.ivy.trace.SpanResult;

public class GuardrailSpan implements SpanInstance<Void> {

  private final String guardrailName;
  private final Supplier<List<Attribute>> attributes;

  public GuardrailSpan(String guardrailName, Supplier<List<Attribute>> attributes) {
    this.guardrailName = guardrailName;
    this.attributes = attributes;
  }

  @Override
  public String name() {
    return guardrailName;
  }

  @Override
  public List<Attribute> attributes() {
    return attributes.get();
  }

  @Override
  public SpanResult result(Void result) {
    return SpanResult.ok(attributes.get().toArray(Attribute[]::new));
  }
}
