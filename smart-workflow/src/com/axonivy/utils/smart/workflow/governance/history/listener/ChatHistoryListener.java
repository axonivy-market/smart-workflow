package com.axonivy.utils.smart.workflow.governance.history.listener;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.axonivy.utils.smart.workflow.governance.history.recorder.internal.ChatHistoryRepository;
import com.axonivy.utils.smart.workflow.governance.history.storage.IvyRepoHistoryStorage;
import com.axonivy.utils.smart.workflow.observability.AiListenerProvider;
import com.axonivy.utils.smart.workflow.utils.IvyVar;

import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.workflow.ICase;
import dev.langchain4j.observability.api.listener.AiServiceListener;

public class ChatHistoryListener implements AiListenerProvider {

  public interface Var {
    String HISTORY_ENABLED = "AI.Observability.Ivy.Enabled";
  }

  private final String agentName;

  public ChatHistoryListener(String agentName) {
    this.agentName = agentName;
  }

  @Override
  public List<AiServiceListener<?>> provide() {
    if (!IvyVar.bool(Var.HISTORY_ENABLED)) {
      return List.of();
    }
    String caseUuid = Ivy.wfCase().uuid();
    String taskUuid = Ivy.wfTask().uuid();
    String agentId = generateAgentId(agentName);

    String processName = getProcessName();

    var repo = new ChatHistoryRepository(caseUuid, taskUuid, agentId, agentName, processName, new IvyRepoHistoryStorage());
    return List.of(
        new AgentResponseListener(repo),
        new ToolExecutionListener(repo),
        new InputGuardrailListener(repo),
        new OutputGuardrailListener(repo));
  }

  private static String generateAgentId(String agentName) {
    return Optional.ofNullable(agentName)
        .filter(s -> !s.isBlank())
        .map(ChatHistoryListener::toUuid)
        .orElseGet(() -> UUID.randomUUID().toString());
  }

  private static String toUuid(String name) {
    try {
      return UUID.fromString(name).toString();
    } catch (IllegalArgumentException e) {
      return UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8)).toString();
    }
  }

  private static String getProcessName() {
    return Optional.ofNullable(Ivy.wfCase())
        .map(ICase::getProcessStart)
        .map(process -> Optional.ofNullable(process.getName())
            .filter(name -> !name.isBlank())
            .orElseGet(process::getRequestPath))
        .orElse("");
  }
}
