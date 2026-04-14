package com.axonivy.utils.smart.workflow.observability.customfields;

import java.util.List;

import com.axonivy.utils.smart.workflow.utils.IvyVar;

import ch.ivyteam.ivy.workflow.ICase;
import ch.ivyteam.ivy.workflow.ITask;
import dev.langchain4j.observability.api.event.AiServiceStartedEvent;
import dev.langchain4j.observability.api.listener.AiServiceListener;
import dev.langchain4j.observability.api.listener.AiServiceStartedListener;

public class CustomFieldTrackingListener implements AiServiceStartedListener {

  public interface Var {
    String PREFIX = "AI.Observability.CustomFields.";
    String ENABLED = PREFIX + "Enabled";
  }

  public static final String AI_ASSISTED = "aiAssisted";

  public List<AiServiceListener<?>> configure() {
    if (!IvyVar.bool(Var.ENABLED)) {
      return List.of();
    }
    return List.of(this);
  }

  @Override
  public void onEvent(AiServiceStartedEvent event) {
    if (ITask.current().customFields().stringField(AI_ASSISTED).get().isEmpty()) {
      ITask.current().customFields().stringField(AI_ASSISTED).set(CustomFieldTrackingValue.SMART_WORKFLOW.name());
    }
    if (ICase.current().customFields().stringField(AI_ASSISTED).get().isEmpty()) {
      ICase.current().customFields().stringField(AI_ASSISTED).set(CustomFieldTrackingValue.SMART_WORKFLOW.name());
    }
  }

}
