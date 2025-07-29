package com.axonivy.utils.ai.memory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.ai.persistence.converter.BusinessEntityConverter;

import ch.ivyteam.ivy.environment.Ivy;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;

public final class PersistentAgentMemoryStore implements ChatMemoryStore, Serializable {

  private static final long serialVersionUID = -1402609847815556646L;
  private static final String IVY_VARIABLE = "AI.AgentMemory";

  private static PersistentAgentMemoryStore instance;

  public static PersistentAgentMemoryStore getInstance() {
    if(instance == null) {
      instance = new PersistentAgentMemoryStore();
    }
    return instance;
  }

  // In-memory cache of chat messages by memory ID
  private Map<Object, List<ChatMessage>> memoryCache;
  
  public PersistentAgentMemoryStore() {
    this.memoryCache = new HashMap<>();
    loadFromIvyVariable();
  }

  @Override
  public List<ChatMessage> getMessages(Object memoryId) {
    if (memoryId == null) {
      return new ArrayList<>();
    }
    
    // First try to get from cache
    List<ChatMessage> messages = memoryCache.get(memoryId);
    if (messages != null) {
      return new ArrayList<>(messages); // Return a copy to prevent external modification
    }
    
    // If not in cache, try to load from Ivy variable
    loadMessagesForMemoryId(memoryId);
    messages = memoryCache.get(memoryId);
    
    return messages != null ? new ArrayList<>(messages) : new ArrayList<>();
  }

  @Override
  public void updateMessages(Object memoryId, List<ChatMessage> messages) {
    if (memoryId == null) {
      return;
    }
    
    // Update cache
    memoryCache.put(memoryId, new ArrayList<>(messages));
    
    // Persist to Ivy variable
    saveToIvyVariable();
  }

  @Override
  public void deleteMessages(Object memoryId) {
    if (memoryId == null) {
      return;
    }
    
    // Remove from cache
    memoryCache.remove(memoryId);
    
    // Persist to Ivy variable
    saveToIvyVariable();
  }
  
  /**
   * Clears all messages from memory and persistence
   */
  public void clearAllMessages() {
    memoryCache.clear();
    saveToIvyVariable();
  }
  
  /**
   * Gets all memory IDs that have stored messages
   */
  public List<Object> getAllMemoryIds() {
    return new ArrayList<>(memoryCache.keySet());
  }
  
  /**
   * Gets the total number of messages across all memory IDs
   */
  public int getTotalMessageCount() {
    return memoryCache.values().stream()
        .mapToInt(List::size)
        .sum();
  }
  
  /**
   * Gets memory statistics
   */
  public Map<Object, Integer> getMemoryStatistics() {
    Map<Object, Integer> stats = new HashMap<>();
    memoryCache.forEach((memoryId, messages) -> stats.put(memoryId, messages.size()));
    return stats;
  }
  
  /**
   * Load all memory data from Ivy variable
   */
  @SuppressWarnings("unchecked")
  private void loadFromIvyVariable() {
    try {
      String jsonMemory = Ivy.var().get(IVY_VARIABLE);
      if (StringUtils.isBlank(jsonMemory)) {
        memoryCache = new HashMap<>();
        return;
      }
      
      // Convert JSON to memory map
      Map<String, List<ChatMessage>> persistedMemory = BusinessEntityConverter.jsonValueToEntity(jsonMemory, Map.class);
      if (persistedMemory != null) {
        memoryCache.clear();
        persistedMemory.forEach((key, value) -> {
          // Convert string keys back to appropriate objects and ensure proper ChatMessage types
          memoryCache.put(key, value);
        });
      }
      
      Ivy.log().debug("Loaded agent memory with " + memoryCache.size() + " memory IDs from AI.AgentMemory variable");
    } catch (Exception e) {
      Ivy.log().error("Failed to load agent memory from AI.AgentMemory variable: " + e.getMessage(), e);
      memoryCache = new HashMap<>();
    }
  }
  
  /**
   * Load messages for a specific memory ID from Ivy variable
   */
  @SuppressWarnings("unchecked")
  private void loadMessagesForMemoryId(Object memoryId) {
    try {
      String jsonMemory = Ivy.var().get(IVY_VARIABLE);
      if (StringUtils.isBlank(jsonMemory)) {
        return;
      }
      
      Map<String, List<ChatMessage>> persistedMemory = BusinessEntityConverter.jsonValueToEntity(jsonMemory, Map.class);
      if (persistedMemory != null && persistedMemory.containsKey(memoryId.toString())) {
        memoryCache.put(memoryId, persistedMemory.get(memoryId.toString()));
      }
    } catch (Exception e) {
      Ivy.log().error("Failed to load messages for memory ID " + memoryId + ": " + e.getMessage(), e);
    }
  }
  
  /**
   * Save all memory data to Ivy variable
   */
  private void saveToIvyVariable() {
    try {
      // Convert memory cache to a serializable format
      Map<String, List<ChatMessage>> serializableMemory = new HashMap<>();
      memoryCache.forEach((key, value) -> {
        serializableMemory.put(key.toString(), value);
      });
      
      String jsonMemory = BusinessEntityConverter.entityToJsonValue(serializableMemory);
      Ivy.var().set(IVY_VARIABLE, jsonMemory);
      
      Ivy.log().debug("Saved agent memory with " + memoryCache.size() + " memory IDs to AI.AgentMemory variable");
    } catch (Exception e) {
      Ivy.log().error("Failed to save agent memory to AI.AgentMemory variable: " + e.getMessage(), e);
    }
  }
  
  /**
   * Force reload from Ivy variable (useful if external changes occurred)
   */
  public void reload() {
    loadFromIvyVariable();
  }
}