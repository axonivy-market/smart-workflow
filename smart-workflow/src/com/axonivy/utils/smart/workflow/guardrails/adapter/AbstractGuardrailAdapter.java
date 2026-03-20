package com.axonivy.utils.smart.workflow.guardrails.adapter;

import com.axonivy.utils.smart.workflow.guardrails.entity.SmartWorkflowGuardrail;

public abstract class AbstractGuardrailAdapter<D extends SmartWorkflowGuardrail> {

  private final D delegate;

  protected AbstractGuardrailAdapter(D delegate) {
    this.delegate = delegate;
  }

  public D getDelegate() {
    return delegate;
  }

  @Override
  public int hashCode() {
    return delegate != null ? delegate.getClass().getName().hashCode() : 0;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    AbstractGuardrailAdapter<?> other = (AbstractGuardrailAdapter<?>) obj;
    if (delegate == null) {
      return other.delegate == null;
    }
    return delegate.getClass().getName().equals(
        other.delegate != null ? other.delegate.getClass().getName() : null);
  }
}
