package com.axonivy.utils.smart.workflow.tools.human;

import java.util.List;
import java.util.stream.Collectors;

import com.axonivy.utils.smart.workflow.memory.IvyMemory;
import com.axonivy.utils.smart.workflow.memory.IvyVolatileStore;

import ch.ivyteam.ivy.persistence.PersistencyException;
import ch.ivyteam.ivy.process.extension.ProgramConfig;
import ch.ivyteam.ivy.process.intermediateevent.IProcessIntermediateEventBean;
import ch.ivyteam.ivy.process.intermediateevent.IProcessIntermediateEventBeanRuntime;
import ch.ivyteam.ivy.scripting.objects.Duration;
import ch.ivyteam.ivy.workflow.ITask;
import ch.ivyteam.ivy.workflow.TaskState;
import ch.ivyteam.ivy.workflow.query.CaseQuery;
import ch.ivyteam.ivy.workflow.query.TaskQuery;
import dev.langchain4j.data.message.ChatMessageType;

public class HumanToolTask implements IProcessIntermediateEventBean {
  
  private IProcessIntermediateEventBeanRuntime runtime;

  @Override
  public void initialize(IProcessIntermediateEventBeanRuntime eventRuntime, ProgramConfig config) {
    eventRuntime.poll().every(new Duration(30));
    this.runtime = eventRuntime;
  }

  @Override
  public void poll() {
    String additionalInformation = "";
    String resultObject = "";
    List<String> pending = pendingHumanTaskCases();
    runtime.getRuntimeLogLogger().info("pending human tasks: " + pending);
    if (pending.isEmpty()) {
      return;
    }
    // TODO read pending
    // check memory which is now complete
    // continue... 

    pending.stream()
      .filter(id -> humanResultAvailable(id))
      .forEach(id -> {
          runtime.getRuntimeLogLogger().debug("human task completed: " + id);
          try {
              runtime.fireProcessIntermediateEventEx(id, resultObject,
                  additionalInformation);
          } catch (PersistencyException ex) {
          }
      });
  }

  private static List<String> pendingHumanTaskCases() {
    CaseQuery withEnvelope = CaseQuery.create().where()
            .customField().stringField("memory.id").isNotNull();
    TaskQuery query = TaskQuery.create().where().state().isEqual(TaskState.WAITING_FOR_INTERMEDIATE_EVENT)
            .and().cases(withEnvelope);
    List<ITask> tasks = query.executor().results();
    List<String> memoryIds = tasks.stream()
            .map(task -> task.getCase().customFields().stringField("memory.id").getOrNull())
            .collect(Collectors.toList());
   // return memoryIds;
    return memoryIds; // tasks.stream().map(ITask::getCase).toList();
  }

  private boolean humanResultAvailable(String id) {
    return new IvyMemory(id, IvyVolatileStore.instance())
        .messages().getLast().type() == ChatMessageType.TOOL_EXECUTION_RESULT;
  }

}
