package com.axonivy.utils.smart.workflow.governance.memory;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

import com.axonivy.utils.smart.workflow.governance.listener.AbstractChatModelListener;
import com.axonivy.utils.smart.workflow.governance.listener.SmartWorkflowChatModelListener;

public class TestSmartWorkflowChatModelListener
    extends AbstractChatModelListenerTest<SmartWorkflowChatModelListener> {

  @Override
  protected SmartWorkflowChatModelListener createListener() {
    return new SmartWorkflowChatModelListener();
  }

  @Test
  void drainPendingClearsState() {
    assertThat(listener.drainPending()).isNull();

    listener.onRequest(null);
    listener.onResponse(buildResponseContext(100, 50));
    listener.drainPending();

    assertThat(listener.drainPending()).isNull();
  }

  @Test
  void onResponseCapturesAllMetadata() {
    listener.onRequest(null);
    listener.onResponse(buildResponseContext(100, 50));

    AbstractChatModelListener.ResponseMetadata meta = listener.drainPending();

    assertThat(meta).isNotNull();
    assertThat(meta.inputTokens()).isEqualTo(100);
    assertThat(meta.outputTokens()).isEqualTo(50);
    assertThat(meta.totalTokens()).isEqualTo(150);
    assertThat(meta.modelName()).isEqualTo("test-model");
    assertThat(meta.durationMs()).isGreaterThanOrEqualTo(0L);
  }

  @Test
  void onResponseWithNullTokenUsageReturnsNullTokenFields() {
    listener.onRequest(null);
    listener.onResponse(buildResponseContextNoTokens());

    AbstractChatModelListener.ResponseMetadata meta = listener.drainPending();

    assertThat(meta).isNotNull();
    assertThat(meta.inputTokens()).isNull();
    assertThat(meta.outputTokens()).isNull();
    assertThat(meta.totalTokens()).isNull();
  }
}
