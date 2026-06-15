package com.axonivy.utils.smart.workflow.client;

import ch.ivyteam.ivy.environment.Ivy;

public class OpenAiTestClient {

  public static String localMockApiUrl(String test) {
    return Ivy.rest().client("mockClient").getUri().toASCIIString() + "/" + test;
  }
}
