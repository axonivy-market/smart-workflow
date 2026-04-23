package com.axonivy.utils.smart.workflow.governance.service;

import org.apache.commons.lang3.StringUtils;

import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.workflow.ITask;

public class TaskService {

  private static final String DISPLAY_NAME_FORMAT = "%s (%s)";
  private static final String NO_NAME_FORMAT = "Task: %s";

  private TaskService() {}

  public static String getDisplayName(String taskUuid) {
    if (StringUtils.isBlank(taskUuid)) {
      return String.format(NO_NAME_FORMAT, taskUuid);
    }
    try {
      ITask task = Ivy.wf().findTask(taskUuid);
      if (task == null) {
        return String.format(NO_NAME_FORMAT, taskUuid);
      }
      String name = task.getName();
      return StringUtils.isNotBlank(name) ?
        String.format(DISPLAY_NAME_FORMAT, name, task.getId()) : Long.toString(task.getId());
    } catch (Exception e) {
      return String.format(NO_NAME_FORMAT, taskUuid);
    }
  }
}
