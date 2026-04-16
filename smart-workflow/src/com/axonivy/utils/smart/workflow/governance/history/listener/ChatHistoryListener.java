package com.axonivy.utils.smart.workflow.governance.history.listener;

import java.util.List;
import java.util.UUID;

import com.axonivy.utils.smart.workflow.governance.history.recorder.internal.ChatHistoryRepository;
import com.axonivy.utils.smart.workflow.governance.history.storage.internal.IvyRepoHistoryStorage;
import com.axonivy.utils.smart.workflow.utils.IvyVar;

import ch.ivyteam.ivy.environment.Ivy;
import dev.langchain4j.observability.api.listener.AiServiceListener;

public class ChatHistoryListener {

  public interface Var {
    String HISTORY_ENABLED = "AI.Observability.Ivy.Enabled";
  }

  public List<AiServiceListener<?>> configure() {
    if (!IvyVar.bool(Var.HISTORY_ENABLED)) {
      return List.of();
    }
    String caseUuid = Ivy.wfCase().uuid();
    String taskUuid = Ivy.wfTask().uuid();
    String agentId = UUID.randomUUID().toString();
    var repo = new ChatHistoryRepository(caseUuid, taskUuid, agentId, new IvyRepoHistoryStorage());
    return List.of(
        new AgentResponseListener(repo),
        new ToolExecutionListener(repo),
        new InputGuardrailListener(repo),
        new OutputGuardrailListener(repo));
  }

}
