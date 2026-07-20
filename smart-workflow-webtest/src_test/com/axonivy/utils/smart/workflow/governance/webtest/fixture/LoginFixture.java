package com.axonivy.utils.smart.workflow.governance.webtest.fixture;

import com.axonivy.ivy.webtest.engine.WebAppFixture;

public interface LoginFixture {

  default void login(WebAppFixture fixture) {
    fixture.login("James", "secret");
  }
}
