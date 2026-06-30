package com.axonivy.utils.smart.workflow.demo;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.security.exec.Sudo;

public abstract class AbstractMockRepository<T> {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  protected abstract String getField();

  protected abstract TypeReference<List<T>> getListType();

  protected abstract List<T> createMockData();

  public void install() {
    if (Ivy.wfCase().customFields().textField(getField()).getOrNull() != null) {
      return;
    }
    Ivy.wfCase().customFields().textField(getField()).set(toJson(createMockData()));
  }

  public void uninstall() {
    Ivy.wfCase().customFields().textField(getField()).set(null);
  }

  public List<T> findAll(String caseUuid) {
    var wfCase = Sudo.get(() -> {
      return Ivy.wf().findCase(caseUuid);
    });
    String json = Optional.ofNullable(wfCase)
      .map(c -> c.customFields().textField(getField()).getOrNull())
      .orElse(null);
    return fromJson(json);
  }

  protected void save(List<T> list) {
    Ivy.wfCase().customFields().textField(getField()).set(toJson(list));
  }

  protected void save(String caseUuid, List<T> list) {
    Sudo.run(() -> {
      var wfCase = Ivy.wf().findCase(caseUuid);
      if (wfCase != null) {
        wfCase.customFields().textField(getField()).set(toJson(list));
      }
    });
  }

  private String toJson(List<?> list) {
    try {
      return MAPPER.writeValueAsString(list);
    } catch (JsonProcessingException e) {
      Ivy.log().error("Failed to serialize " + getField(), e);
      return "[]";
    }
  }

  private List<T> fromJson(String json) {
    if (json == null || json.isBlank()) {
      return Collections.emptyList();
    }
    try {
      return MAPPER.readValue(json, getListType());
    } catch (JsonProcessingException e) {
      Ivy.log().error("Failed to deserialize " + getField(), e);
      return Collections.emptyList();
    }
  }
}
