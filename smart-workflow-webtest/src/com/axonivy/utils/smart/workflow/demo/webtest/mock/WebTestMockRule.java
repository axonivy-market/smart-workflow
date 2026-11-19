package com.axonivy.utils.smart.workflow.demo.webtest.mock;

public record WebTestMockRule(
    String requestContains,
    Boolean toolResult,
    String answer,
    String toolName,
    String toolArguments,
    Failure failure) {

  public static final String TOOL_RESULT = "{{toolResult}}";
  public static final String UNIQUE_ID = "{{uniqueId}}";

  public enum Failure {
    INVALID_API_KEY,
    RATE_LIMIT,
    SERVER_ERROR
  }

  public static WebTestMockRule whenRequestContains(String text) {
    return new WebTestMockRule(text, null, null, null, null, null);
  }

  public static WebTestMockRule always() {
    return whenRequestContains(null);
  }

  public WebTestMockRule withToolResult(boolean present) {
    return new WebTestMockRule(requestContains, present, answer, toolName, toolArguments, failure);
  }

  public WebTestMockRule thenAnswer(String text) {
    return new WebTestMockRule(requestContains, toolResult, text, null, null, null);
  }

  /** @param arguments the tool's arguments as a JSON object */
  public WebTestMockRule thenCallTool(String name, String arguments) {
    return new WebTestMockRule(requestContains, toolResult, null, name, arguments, null);
  }

  public WebTestMockRule thenFail(Failure kind) {
    return new WebTestMockRule(requestContains, toolResult, null, null, null, kind);
  }
}
