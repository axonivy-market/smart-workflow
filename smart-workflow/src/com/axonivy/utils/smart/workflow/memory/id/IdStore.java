package com.axonivy.utils.smart.workflow.memory.id;

import java.util.Optional;

public interface IdStore {
  Optional<String> id();
  void id(String id);
}
