package com.axonivy.utils.ai.tools;

import java.util.Objects;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;

import ch.ivyteam.ivy.process.call.SubProcessCall;
import ch.ivyteam.ivy.process.call.SubProcessCallResult;
import ch.ivyteam.ivy.process.model.element.event.start.CallSubStart;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.internal.Json;

@SuppressWarnings("restriction")
public class IvyToolExecutor {

  public static ToolExecutionResultMessage execute(ToolExecutionRequest execTool) {
    String name = execTool.name();

    Optional<CallSubStart> startable = IvyToolsProcesses.toolStarts().stream()
        .filter(start -> start.getSignature().getName().equals(name))
        .findFirst();

    if (startable.isEmpty()) {
      // TODO: how does Agentic error handling look like?
      return ToolExecutionResultMessage.from(execTool, "failed to execut tool; unknown ivy-process function");
    }

    var signature = startable.get().getSignature();

    var callable = SubProcessCall
        .withPid(startable.get().getRootProcess().getPid().getProcessGuid())
        .withStartSignature(signature.toSimpleNameSignature());

    var args = Json.fromJson(execTool.arguments(), JsonNode.class);
    execTool.arguments();

    var values = signature.getInputParameters().stream().map(in -> {
      var jArg = args.get(in.getName());
      // TODO; support much more scenarios!
      if ("String".equals(in.getType().getSimpleName())) {
        if (jArg != null) {
          return jArg.asText();
        }
        return "";
      }
      return null;
    })
        .filter(Objects::nonNull)
        .toArray(Object[]::new);

    SubProcessCallResult res = callable.call(values);
    Object miniResult = res.first();
    return ToolExecutionResultMessage.from(execTool, Json.toJson(miniResult));
  }
}
