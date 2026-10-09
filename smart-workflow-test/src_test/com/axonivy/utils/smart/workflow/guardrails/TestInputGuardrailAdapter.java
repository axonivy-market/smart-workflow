package com.axonivy.utils.smart.workflow.guardrails;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.axonivy.utils.smart.workflow.guardrails.adapter.InputGuardrailAdapter;
import com.axonivy.utils.smart.workflow.guardrails.entity.GuardrailResult;
import com.axonivy.utils.smart.workflow.guardrails.entity.SmartWorkflowInputGuardrail;

import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.guardrail.GuardrailRequestParams;
import dev.langchain4j.guardrail.GuardrailResult.Failure;
import dev.langchain4j.guardrail.GuardrailResult.Result;
import dev.langchain4j.guardrail.InputGuardrailRequest;
import dev.langchain4j.invocation.InvocationContext;

class TestInputGuardrailAdapter {

  record Case(GuardrailResult delegateResult, Result expectedResult, String expectedSuccessfulText,
      String expectedFailureMessage) {}

  @Test
  void validateUserMessage_returnsResultsFromDelegate() {
    var cases = List.of(
        new Case(GuardrailResult.allow(), Result.SUCCESS, null, null),
        new Case(GuardrailResult.block("blocked reason"), Result.FAILURE, null, "blocked reason"),
        new Case(GuardrailResult.allowWithRewrite("rewritten"), Result.SUCCESS_WITH_RESULT, "rewritten", null));

    for (var testCase : cases) {
      var adapter = new InputGuardrailAdapter(_ -> testCase.delegateResult());

      var result = adapter.validate(UserMessage.from(TextContent.from("hello")));

      assertThat(result.result()).as("case %s", testCase).isEqualTo(testCase.expectedResult());
      assertThat(result.successfulText()).as("case %s", testCase).isEqualTo(testCase.expectedSuccessfulText());
      if (testCase.expectedFailureMessage() != null) {
        List<Failure> failures = result.failures();
        assertThat(failures).extracting(Failure::message).containsExactly(testCase.expectedFailureMessage());
      }
    }
  }

  @Test
  void validateUserMessage_doesNotThrowOnMultimodalMessage_regressionForSingleTextBug() {
    String[] capturedMessage = new String[1];
    var adapter = new InputGuardrailAdapter(message -> {
      capturedMessage[0] = message;
      return GuardrailResult.allow();
    });
    var multimodal = UserMessage.from(
        TextContent.from("describe this:"),
        ImageContent.from("http://example.com/img.png", "image/png"));

    var result = adapter.validate(multimodal);

    assertThat(result.result()).isEqualTo(Result.SUCCESS);
    assertThat(capturedMessage[0]).isEqualTo("describe this:\n<file: IMAGE>");
  }

  @Test
  void validateRequest_passesInvocationIdToDelegate() {
    UUID invocationId = UUID.randomUUID();
    String[] capturedInvocationId = new String[1];
    SmartWorkflowInputGuardrail guardrail = new SmartWorkflowInputGuardrail() {
      @Override
      public GuardrailResult evaluate(String message) {
        return evaluate(message, null);
      }

      @Override
      public GuardrailResult evaluate(String message, String invocationIdParam) {
        capturedInvocationId[0] = invocationIdParam;
        return GuardrailResult.allow();
      }
    };
    var adapter = new InputGuardrailAdapter(guardrail);
    var request = InputGuardrailRequest.builder()
        .userMessage(UserMessage.from(TextContent.from("hello")))
        .commonParams(GuardrailRequestParams.builder()
            .userMessageTemplate("")
            .variables(Map.of())
            .invocationContext(InvocationContext.builder()
                .invocationId(invocationId)
                .timestamp(Instant.now())
                .methodName("chat")
                .interfaceName("ChatAgent")
                .build())
            .build())
        .build();

    adapter.validate(request);

    assertThat(capturedInvocationId[0]).isEqualTo(invocationId.toString());
  }
}
