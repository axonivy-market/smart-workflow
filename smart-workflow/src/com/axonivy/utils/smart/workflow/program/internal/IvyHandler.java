package com.axonivy.utils.smart.workflow.program.internal;

import ch.ivyteam.ivy.bpm.error.BpmError;
import dev.langchain4j.service.tool.ToolErrorContext;
import dev.langchain4j.service.tool.ToolErrorHandlerResult;
import dev.langchain4j.service.tool.ToolExecutionErrorHandler;

public class IvyHandler implements ToolExecutionErrorHandler {

  @Override
  public ToolErrorHandlerResult handle(Throwable throwable, ToolErrorContext context) {
    // TODO Auto-generated method stub
   // throw new UnsupportedOperationException("Unimplemented method 'handle'");
    if (throwable instanceof BpmError error) {
      if (error.getErrorCode().equals("human:decision")){
        return ToolErrorHandlerResult.text("human decision required");
      }
      System.out.println(error);
    }

   // default
    String errorMessage = isNullOrBlank(throwable.getMessage()) ? throwable.getClass().getName() : throwable.getMessage();
    return ToolErrorHandlerResult.text(errorMessage);
  }

  private static boolean isNullOrBlank(String message) {
    return message == null || message.isBlank();
  }

}
