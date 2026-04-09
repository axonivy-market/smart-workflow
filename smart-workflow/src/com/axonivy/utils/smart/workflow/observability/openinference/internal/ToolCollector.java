package com.axonivy.utils.smart.workflow.observability.openinference.internal;

import java.util.LinkedList;
import java.util.List;

import com.arize.semconv.trace.SemanticConventions;
import com.axonivy.utils.smart.workflow.observability.openinference.OpenInferenceTracing.MessageOptions;

import ch.ivyteam.ivy.trace.Attribute;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.observability.api.event.ToolExecutedEvent;

public class ToolCollector {

  private final MessageOptions options;
  private List<Attribute> attrs = new LinkedList<>();

  public ToolCollector(MessageOptions options) {
    this.options = options;
  }

  public List<Attribute> getAttributes() {
    return attrs;
  }

  public void onRequestExecution(ToolExecutionRequest request) {
    attrs.add(Attribute.attribute(SemanticConventions.OPENINFERENCE_SPAN_KIND,
        SemanticConventions.OpenInferenceSpanKind.TOOL.getValue())
    );
  }

  public void onExecuted(ToolExecutedEvent event) {
    attrs.addAll(List.of(
        Attribute.attribute(
            SemanticConventions.TOOL_NAME, event.request().name()),
        Attribute.attribute(
            SemanticConventions.TOOL_CALL_ID, event.request().id())));
    if (!options.hideInput()) {
      attrs.addAll(List.of(
          Attribute.attribute(SemanticConventions.INPUT_VALUE,
              event.request().arguments()),
          Attribute.attribute(SemanticConventions.INPUT_MIME_TYPE,
              SemanticConventions.MimeType.JSON.getValue()),
          Attribute.attribute(SemanticConventions.TOOL_PARAMETERS,
              event.request().arguments())));
    }
    if (!options.hideOutput()) {
      attrs.addAll(List.of(
          Attribute.attribute(SemanticConventions.OUTPUT_VALUE,
              event.resultText()),
          Attribute.attribute(SemanticConventions.OUTPUT_MIME_TYPE,
              SemanticConventions.MimeType.TEXT.getValue())));
    }
  }

}
