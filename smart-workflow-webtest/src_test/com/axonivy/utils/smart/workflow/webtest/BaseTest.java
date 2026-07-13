package com.axonivy.utils.smart.workflow.webtest;

import com.axonivy.ivy.webtest.engine.WebAppFixture;

public abstract class BaseTest {

  protected void login(WebAppFixture fixture) {
    fixture.login("James", "secret");
  }
}
