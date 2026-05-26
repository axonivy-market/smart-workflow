package com.axonivy.utils.smart.workflow.program.internal;

import ch.ivyteam.ivy.bpm.error.BpmError;
import dev.langchain4j.service.tool.ToolErrorContext;
import dev.langchain4j.service.tool.ToolErrorHandlerResult;
import dev.langchain4j.service.tool.ToolExecutionErrorHandler;

public class IvyToolErrorHandler implements ToolExecutionErrorHandler {

  @Override
  public ToolErrorHandlerResult handle(Throwable throwable, ToolErrorContext context) {
    if (throwable instanceof BpmError error) {
      throw enrichToolContext(error, context);
    }
    String errorMessage = isNullOrBlank(throwable.getMessage()) ? throwable.getClass().getName() : throwable.getMessage();
    return ToolErrorHandlerResult.text(errorMessage);
  }

  private BpmError enrichToolContext(BpmError error, ToolErrorContext context) {
    var execTool = context.toolExecutionRequest();
    return BpmError.create(error)
        .withAttribute("tool.id", execTool.id())
        .withAttribute("tool.name", execTool.name())
        .withAttribute("tool.arguments", execTool.arguments())
        .withAttribute("ai.invocationId", context.invocationContext().invocationId())
        .build();
  }

  private static boolean isNullOrBlank(String message) {
    return message == null || message.isBlank();
  }

}
