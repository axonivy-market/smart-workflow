package com.axonivy.utils.smart.workflow.governance.history.listener;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.smart.workflow.governance.history.recorder.internal.ChatHistoryRepository;
import com.axonivy.utils.smart.workflow.governance.history.storage.internal.IvyRepoHistoryStorage;
import com.axonivy.utils.smart.workflow.observability.AiListenerProvider;
import com.axonivy.utils.smart.workflow.utils.IvyVar;

import ch.ivyteam.ivy.environment.Ivy;
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
    String agentId = StringUtils.isBlank(agentName)
        ? UUID.randomUUID().toString()
        : UUID.nameUUIDFromBytes(agentName.getBytes(StandardCharsets.UTF_8)).toString();

    String processName = Optional.ofNullable(Ivy.wfCase())
        .map(c -> c.getProcessStart())
        .map(ps -> StringUtils.defaultIfBlank(ps.getName(), ps.getRequestPath()))
        .orElse("");

    var repo = new ChatHistoryRepository(caseUuid, taskUuid, agentId, agentName, processName, new IvyRepoHistoryStorage());
    return List.of(
        new AgentResponseListener(repo),
        new ToolExecutionListener(repo),
        new InputGuardrailListener(repo),
        new OutputGuardrailListener(repo));
  }

}
