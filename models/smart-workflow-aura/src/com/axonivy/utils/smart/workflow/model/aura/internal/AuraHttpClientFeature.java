package com.axonivy.utils.smart.workflow.model.aura.internal;

import org.apache.hc.client5.http.impl.DefaultHttpRequestRetryStrategy;
import org.apache.hc.core5.util.TimeValue;

import jakarta.ws.rs.core.Feature;
import jakarta.ws.rs.core.FeatureContext;

/** Disables the Ivy Apache connector's automatic resubmission of billable requests. */
public final class AuraHttpClientFeature implements Feature {

  @Override
  public boolean configure(FeatureContext context) {
    context.property("jersey.config.apache5.client.retryStrategy",
        new DefaultHttpRequestRetryStrategy(0, TimeValue.ZERO_MILLISECONDS));
    return true;
  }
}
