package com.axonivy.utils.smart.workflow.observability.openinference.internal;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.arize.semconv.trace.SemanticConventions;

import dev.langchain4j.guardrail.GuardrailResult.Failure;
import dev.langchain4j.observability.api.event.GuardrailExecutedEvent;

public class GuardrailRecorder {

  public Map<String, Object> handleGuardrail(GuardrailExecutedEvent<?, ?, ?> event,
      String type, String validatedMessage, String guardrailName) {
    Map<String, Object> attrs = new LinkedHashMap<>();
    attrs.put(SemanticConventions.OPENINFERENCE_SPAN_KIND,
        SemanticConventions.OpenInferenceSpanKind.GUARDRAIL.getValue());

    attrs.put("validator_name", guardrailName);
    // Smart Workflow adapters always throw on block (InputGuardrailAdapter uses failure(),
    // OutputGuardrailAdapter uses fatal()), so on_fail is always "exception".
    // If we support retry/reprompt in the future, derive from OutputGuardrailResult.isRetry()/isReprompt().
    attrs.put("validator_on_fail", "exception");

    var result = event.result();
    attrs.put("guardrail.type", type);
    attrs.put("guardrail.result", result.result().name());

    if (validatedMessage != null) {
      attrs.put(SemanticConventions.INPUT_VALUE, validatedMessage);
      attrs.put(SemanticConventions.INPUT_MIME_TYPE, "text/plain");
    }

    boolean passed = result.failures().isEmpty();
    if (passed) {
      attrs.put(SemanticConventions.OUTPUT_VALUE, "pass");
    } else {
      attrs.put(SemanticConventions.OUTPUT_VALUE, "fail");
      List<String> messages = new ArrayList<>();
      for (var failure : result.failures()) {
        if (failure instanceof Failure f && f.message() != null) {
          messages.add(f.message());
        }
      }
      if (!messages.isEmpty()) {
        attrs.put("guardrail.failure_message", String.join("; ", messages));
      }
    }
    attrs.put(SemanticConventions.OUTPUT_MIME_TYPE, "text/plain");

    return attrs;
  }
}
