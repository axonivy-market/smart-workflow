package com.axonivy.utils.smart.workflow.observability.customfields;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.axonivy.utils.ai.mock.MockOpenAI;
import com.axonivy.utils.smart.workflow.client.OpenAiTestClient;
import com.axonivy.utils.smart.workflow.model.ChatModelFactory.AiConf;
import com.axonivy.utils.smart.workflow.model.openai.OpenAiModelProvider;
import com.axonivy.utils.smart.workflow.model.openai.internal.OpenAiServiceConnector.OpenAiConf;

import ch.ivyteam.ivy.bpm.engine.client.BpmClient;
import ch.ivyteam.ivy.bpm.engine.client.ExecutionResult;
import ch.ivyteam.ivy.bpm.engine.client.element.BpmProcess;
import ch.ivyteam.ivy.environment.AppFixture;
import ch.ivyteam.ivy.workflow.CaseState;
import ch.ivyteam.ivy.workflow.ICase;
import ch.ivyteam.ivy.workflow.ITask;
import ch.ivyteam.test.RestResourceTest;
import ch.ivyteam.test.resource.ResourceResponder;

@RestResourceTest
class TestCustomFieldTrackingListener {

  private static final BpmProcess MULTI_TASK = BpmProcess.name("MultiTaskProcesses");
  private static final String AI_ASSISTED = CustomFieldTrackingListener.AI_ASSISTED;
  private static final String SMART_WORKFLOW = CustomFieldTrackingValue.SMART_WORKFLOW.name();

  @BeforeEach
  void setup(AppFixture fixture, ResourceResponder responder) {
    fixture.var(AiConf.DEFAULT_PROVIDER, OpenAiModelProvider.NAME);
    fixture.var(OpenAiConf.BASE_URL, OpenAiTestClient.localMockApiUrl("customfields"));
    fixture.var(OpenAiConf.API_KEY, "");
    Queue<String> responses = new ArrayDeque<>(List.of(
        "multiTaskDemo_r1.json",
        "multiTaskDemo_r2.json",
        "multiTaskDemo_r3.json",
        "multiTaskDemo_r4.json",
        "multiTaskDemo_r5.json"
    ));
    MockOpenAI.defineChat(_ -> responder.send(responses.poll()));
  }

  @Test
  void doesNotMarkTasksOrCaseWhenDisabled(BpmClient client, AppFixture fixture) {
    fixture.var(CustomFieldTrackingListener.Var.ENABLED, "false");

    var res = runMultiTaskDemoToCompletion(client);

    ICase icase = res.workflow().activeCase();
    assertThat(icase.customFields().stringField(AI_ASSISTED).get())
        .as("case should not be marked when custom field tracking is disabled")
        .isEmpty();

    icase.tasks().all().forEach(task ->
        assertThat(task.customFields().stringField(AI_ASSISTED).get())
            .as("task '%s' should not be marked when custom field tracking is disabled", task.getName())
            .isEmpty());
  }

  @Test
  void marksTasksAndCaseWhenEnabled(BpmClient client) {
    var res = runMultiTaskDemoToCompletion(client);

    ICase icase = res.workflow().activeCase();
    assertThat(icase.getState()).isEqualTo(CaseState.DONE);
    assertThat(icase.customFields().stringField(AI_ASSISTED).get())
        .as("case should be marked as AI assisted")
        .contains(SMART_WORKFLOW);

    var customFieldsByTaskName = icase.tasks().all().stream()
        .collect(Collectors.toMap(
            ITask::getName,
            task -> task.customFields().stringField(AI_ASSISTED).get().orElse(""),
            (_, second) -> second));

    assertThat(customFieldsByTaskName)
        .as("workflow tasks assisted by the AI element should be marked")
        .containsEntry("AI Order Processing Demo", "") // this task is not assisted by AI
        .containsEntry("Task 1: Order Analysis", SMART_WORKFLOW)
        .containsEntry("Task 2: Invoice Generation", SMART_WORKFLOW)
        .containsEntry("Task 3: Review and Finalization", SMART_WORKFLOW);
  }

  private ExecutionResult runMultiTaskDemoToCompletion(BpmClient client) {
    var res = client.start()
        .process(MULTI_TASK.elementName("multiTaskDemo"))
        .as().systemUser().execute();
    while (res.workflow().anyActiveTask().isPresent()) {
      res = client.start().anyActiveTask(res).as().systemUser().execute();
    }
    return res;
  }
}
