package com.axonivy.utils.smart.workflow.tools.internal;

import java.util.Map;
import java.util.Optional;

import ch.ivyteam.ivy.process.call.SubProcessCallResult;
import ch.ivyteam.ivy.process.call.SubProcessCallStartEvent;
import ch.ivyteam.ivy.process.call.SubProcessCallStartParamCaller;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.internal.Json;

@SuppressWarnings("restriction")
public class IvySubProcessToolExecutor {

  public static ToolExecutionResultMessage execute(ToolExecutionRequest execTool) {
    String name = execTool.name();

    Optional<SubProcessCallStartEvent> startable = IvyToolsProcesses
        .toolStarts().stream()
        .filter(start -> start.description().name().equals(name))
        .findFirst();

    if (startable.isEmpty()) {
      // TODO: how does Agentic error handling look like?
      return ToolExecutionResultMessage.from(execTool, "failed to execut tool; unknown ivy-process function");
    }

    var parameters = new JsonProcessParameters()
        .readParams(startable.get().description().in(), execTool.arguments());

    SubProcessCallResult res = call(startable.get(), parameters);

    return ToolExecutionResultMessage.from(execTool, Json.toJson(res.asMap()));
  }

  @SuppressWarnings("null")
  private static SubProcessCallResult call(SubProcessCallStartEvent callable, Map<String, Object> params) {
    if (params.isEmpty()) {
      return callable.call();
    }
    SubProcessCallStartParamCaller pCaller = null;
    for (var entry : params.entrySet()) {
      pCaller = callable.withParam(entry.getKey(), entry.getValue());
    }
    return pCaller.call();
  }

}
