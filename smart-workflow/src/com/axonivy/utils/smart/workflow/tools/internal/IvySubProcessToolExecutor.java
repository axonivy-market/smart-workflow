package com.axonivy.utils.smart.workflow.tools.internal;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.axonivy.utils.smart.workflow.tools.provider.SmartWorkflowTool.ToolParameter;

import ch.ivyteam.ivy.bpm.error.BpmError;
import ch.ivyteam.ivy.process.call.StartParameter;
import ch.ivyteam.ivy.process.call.SubProcessCallResult;
import ch.ivyteam.ivy.process.call.SubProcessCallStartEvent;
import ch.ivyteam.ivy.process.call.SubProcessCallStartParamCaller;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.exception.ToolArgumentsException;
import dev.langchain4j.internal.Json;
import dev.langchain4j.service.tool.ToolErrorContext;
import dev.langchain4j.service.tool.ToolErrorHandlerResult;
import dev.langchain4j.service.tool.ToolExecutionResult;
import opennlp.tools.stemmer.snowball.englishStemmer;

public class IvySubProcessToolExecutor {

  public static ToolExecutionResultMessage execute(ToolExecutionRequest execTool) {
    String name = execTool.name();

    Optional<SubProcessCallStartEvent> startable = IvyToolsProcesses
        .toolStarts().stream()
        .filter(start -> start.description().name().equals(name))
        .findFirst();

    if (startable.isEmpty()) {
      // TODO: how does Agentic error handling look like?
      return ToolExecutionResultMessage.from(execTool, "failed to execute tool; unknown ivy-process function");
    }

    List<ToolParameter> toolParams = startable.get().description().in().stream()
        .map((StartParameter p) -> new ToolParameter(p.name(), p.description(), p.typeName()))
        .toList();
    var parameters = new JsonProcessParameters()
        .readParams(toolParams, execTool.arguments());

        try {
          SubProcessCallResult res = call(startable.get(), parameters);
          return ToolExecutionResultMessage.from(execTool, Json.toJson(res.asMap()));
        } catch (BpmError error) {
          ToolErrorContext errorContext = ToolErrorContext.builder()
                    .toolExecutionRequest(execTool)
                   // .invocationContext(invocationContext)
                    .build();
          throw error;
            // ToolErrorHandlerResult errorHandlerResult;
            // if (e instanceof ToolArgumentsException) {
            //     errorHandlerResult = argumentsErrorHandler.handle(getCause(e), errorContext);
            // } else {
            //     errorHandlerResult = executionErrorHandler.handle(getCause(e), errorContext);
            // }

            // return ToolExecutionResult.builder()
            //         .isError(true)
            //         .resultText("failed to execute tool; BPM error occurred")
            //         .build();
        }
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
    try {
      return pCaller.call();
    } catch(Exception ex) {
      if (ex.getCause() instanceof BpmError error) {
        if (error.getId().equals("human:task")) {
          throw error;
        }
      }
      throw ex;
    }
  }

}
