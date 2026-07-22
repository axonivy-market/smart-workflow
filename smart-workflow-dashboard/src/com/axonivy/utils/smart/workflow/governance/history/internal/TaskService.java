package com.axonivy.utils.smart.workflow.governance.history.internal;

import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.security.exec.Sudo;
import ch.ivyteam.ivy.workflow.ITask;

public class TaskService {

  private static final String DISPLAY_NAME_FORMAT = "%s (%s)";
  private static final String NO_NAME_FORMAT = "Task: %s";

  private TaskService() {}

  public static ITask findTask(String taskUuid) {
    try {
      return Sudo.get(() -> Ivy.wf().findTask(taskUuid));
    } catch (Exception e) {
      return null;
    }
  }

  public static String getDisplayName(String taskUuid) {
    ITask task = findTask(taskUuid);
    if (task == null) {
      return String.format(NO_NAME_FORMAT, taskUuid);
    }
    String name = task.getName();
    return (name == null || name.isBlank())
        ? String.format(NO_NAME_FORMAT, task.getId())
        : String.format(DISPLAY_NAME_FORMAT, name, task.getId());
  }
}
