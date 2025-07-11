package com.axonivy.utils.ai.history;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.commons.collections4.CollectionUtils;

import ch.ivyteam.ivy.environment.Ivy;

public class HistoryLog {

  private List<HistoryRecord> records;

  public List<HistoryRecord> getRecords() {
    return records;
  }

  public void setRecords(List<HistoryRecord> records) {
    this.records = records;
  }

  private enum Actor {
    USER, AI, SYSTEM;
  }

  public record HistoryRecord(int stepNo, Actor actor, String content) {
  };

  public void addAiMessage(String message, int stepNo) {
    if (CollectionUtils.isEmpty(records)) {
      records = new ArrayList<>();
    }
    records.add(new HistoryRecord(stepNo, Actor.AI, message));
  }

  public void addUserMessage(String message) {
    if (CollectionUtils.isEmpty(records)) {
      records = new ArrayList<>();
    }
    records.add(new HistoryRecord(0, Actor.USER, message));
  }

  public void addSystemMessage(String message, int stepNo) {
    if (CollectionUtils.isEmpty(records)) {
      records = new ArrayList<>();
    }
    HistoryRecord x = new HistoryRecord(stepNo, Actor.SYSTEM, message);
    Ivy.log().error(x);
    records.add(x);
  }

  public HistoryRecord getRecordOfStep(int stepNo) {
    if (CollectionUtils.isEmpty(records)) {
      return null;
    }
    return records.stream().filter(r -> r.stepNo == stepNo).findFirst().get();
  }

  public List<HistoryRecord> getRecordsOfSteps(List<Integer> stepNos) {
    List<HistoryRecord> result = new ArrayList<>();
    if (CollectionUtils.isNotEmpty(stepNos)) {
      for (Integer stepNo : stepNos) {
        result.add(records.stream().filter(history -> history.stepNo == stepNo).findFirst().get());
      }
    }
    return result.stream().filter(Objects::nonNull).toList();
  }
}