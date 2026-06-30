package ch.ivyteam.test;

import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import ch.ivyteam.ivy.server.internal.test.AppFixtureJu5Context;

/**
 * Disables CustomFieldTrackingListener for all {@link RestResourceTest} tests by default.
 * Tests that specifically verify tracking behavior must re-enable it via {@code AppFixture.var(ENABLED, "true")}.
 */
class DisableCustomFieldTrackingExtension implements BeforeEachCallback {

  @Override
  public void beforeEach(ExtensionContext context) {
    AppFixtureJu5Context.get(context).getFixture()
        .var("AI.Observability.CustomFields.Enabled", "false");
  }
}
