package com.axonivy.utils.smart.workflow.tools.human;

import com.axonivy.utils.smart.workflow.utils.JsonUtils;

import ch.ivyteam.ivy.bpm.error.BpmError;
import ch.ivyteam.ivy.environment.Ivy;

public class ErrorHandler {

  private final BpmError error;

  public ErrorHandler(BpmError error) {
    this.error = error;
  }

  public <T> T args(Class<T> clazz) {
    String args = (String) error.getAttribute("tool.arguments");
    try {
      var ref = JsonUtils.getObjectMapper().readTree(args);
      var dec = ref.get("decision");

      T decision = JsonUtils.getObjectMapper()
        .readerFor(clazz).readValue(dec.toPrettyString());
        System.out.println(decision);
        Ivy.log().info("decision: " + decision);
        return (T) decision;
    } catch (Exception e) {
      Ivy.log().error("Failed to parse human decision from error arguments", e);
      return null;
    }
  }
}
