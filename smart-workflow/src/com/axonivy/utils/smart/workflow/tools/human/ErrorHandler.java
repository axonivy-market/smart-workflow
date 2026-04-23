package com.axonivy.utils.smart.workflow.tools.human;

import com.axonivy.utils.smart.workflow.memory.IvyMemory;
import com.axonivy.utils.smart.workflow.memory.IvyVolatileStore;
import com.axonivy.utils.smart.workflow.utils.JsonUtils;

import ch.ivyteam.ivy.bpm.error.BpmError;
import ch.ivyteam.ivy.environment.Ivy;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;

public class ErrorHandler {

  private final BpmError error;

  public ErrorHandler(BpmError error) {
    this.error = error;
  }

  /**
   * YAGNI -> error can propagate concrete params via attribute object!
   * @param <T>
   * @param clazz
   * @return
   */
  public <T> T args(Class<T> clazz) {
    String args = (String) error.getAttribute("tool.arguments");
    try {
      var ref = JsonUtils.getObjectMapper().readTree(args);
      var dec = ref.get("decision"); // TODO: generic selection of arguments from the tool...

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

  public void resolve(String decision) {
    error.setAttribute("tool.decision", decision);
    // TODO serious injection of user response into next agent call memory?
    IvyMemory memory = IvyMemory.of(Ivy.wfCase());
    var b4 = memory.messages();
    var invoke = b4.getLast();
    if (invoke instanceof AiMessage ai) {
      // TODO: serious of tool execution requests that were interrupted in order to be solved by the user
      var request = ai.toolExecutionRequests().get(0);
      ToolExecutionResultMessage msg = ToolExecutionResultMessage.from(request, decision);
      memory.add(msg);
    }
  }

  public void resolveIntermediate(String decision) {
    //error.setAttribute("tool.decision", decision);
    // TODO serious injection of user response into next agent call memory?
    IvyMemory memory = new IvyMemory(String.valueOf(Ivy.wfCase().getId()), IvyVolatileStore.instance());
    Ivy.log().info("Resolving human task memory: " + memory);
    var b4 = memory.messages();
    var invoke = b4.get(b4.size()-3);
    Ivy.log().info("Last message in memory before resolving human task: " + invoke);
    if (invoke instanceof AiMessage ai) {
      // TODO: serious of tool execution requests that were interrupted in order to be
      // solved by the user
      var request = ai.toolExecutionRequests().get(0);
      ToolExecutionResultMessage msg = ToolExecutionResultMessage.from(request, decision);
      memory.add(msg);
      Ivy.log().info("Added decision to memory: "+memory);
    }
  }
}
