package com.axonivy.utils.smart.workflow.governance.memory;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import ch.ivyteam.ivy.environment.Ivy;

public class ChatMemoryRepository {

  private static final String FIELD_AGENT_ID = "agentId";

  public static List<ChatMemoryEntry> findAll() {
    return Ivy.repo().search(ChatMemoryEntry.class).execute().getAll();
  }

  public static List<ChatMemoryEntry> findByAgentId(String agentId) {
    if (StringUtils.isBlank(agentId)) {
      return findAll();
    }
    return Ivy.repo().search(ChatMemoryEntry.class)
        .textField(FIELD_AGENT_ID)
        .isEqualToIgnoringCase(agentId)
        .execute()
        .getAll();
  }

  public static List<String> distinctAgentIds() {
    return findAll().stream()
        .map(ChatMemoryEntry::getAgentId)
        .filter(StringUtils::isNotBlank)
        .distinct()
        .sorted()
        .toList();
  }

  public static void delete(ChatMemoryEntry entry) {
    if (entry != null) {
      Ivy.repo().delete(entry);
    }
  }
}