package com.axonivy.utils.smart.workflow.hibernate;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.axonivy.utils.ai.mock.MockOpenAI;
import com.axonivy.utils.smart.workflow.client.OpenAiTestClient;
import com.axonivy.utils.smart.workflow.human.HumanDecision;
import com.axonivy.utils.smart.workflow.memory.store.BusinessDataMemory;
import com.axonivy.utils.smart.workflow.model.openai.internal.OpenAiServiceConnector.OpenAiConf;
import com.fasterxml.jackson.databind.JsonNode;

import Patterns.HumanInTheLoop.HibernationData;
import ch.ivyteam.ivy.bpm.engine.client.BpmClient;
import ch.ivyteam.ivy.bpm.engine.client.element.BpmProcess;
import ch.ivyteam.ivy.environment.AppFixture;
import ch.ivyteam.ivy.workflow.ITask;
import ch.ivyteam.test.RestResourceTest;
import ch.ivyteam.test.resource.ResourceResponder;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageType;

@RestResourceTest
public class TestHumanInTheLoop {

  private static final BpmProcess HIBERNATION_PLAYGROUND = BpmProcess.name("Hibernation");
  private final ResourceResponder responder = new ResourceResponder(getClass());

  @BeforeEach
  void setup(AppFixture fixture) {
    fixture.var(OpenAiConf.BASE_URL, OpenAiTestClient.localMockApiUrl("tool-demo"));
    fixture.var(OpenAiConf.API_KEY, "");
  }

  @Test
  void askUserOptions_escalation(BpmClient client) {
    MockOpenAI.defineChat(this::humanInTheLoopChat);
    client.mock()
        .uiOf(HIBERNATION_PLAYGROUND.elementName("UserFeedback"))
        .with((params, results) -> {
          var decision = (HumanDecision) params.get("decision");
          assertThat(decision.getQuestion())
              .isEqualTo("Which ice cream would you like to eat today?");
          results.set("first", decision.getOptions().getFirst());
        });
    var result = client.start()
        .process(HIBERNATION_PLAYGROUND.elementName("joinedForces"))
        .execute();

    ITask initTask = result.workflow().activeTasks().getLast();
    assertThat(initTask.getName())
        .as("Workflow task involves human in the ongoing AI conversion")
        .isEqualTo("Assisted: Which ice cream would you like to eat today?");

    HibernationData initData = result.data().last();
    var memoryMessages = new BusinessDataMemory().getMessages(initData.getAiMemoryId());
    assertThat(memoryMessages)
        .extracting(ChatMessage::type)
        .as("Agent memory is persistent; to survive engine reboots and long running tasks.")
        .containsExactly(ChatMessageType.SYSTEM, ChatMessageType.USER, ChatMessageType.AI);

    var finished = client.start()
        .anyActiveTask(result)
        .as().everybody()
        .execute();
    HibernationData finalData = finished.data().last();
    assertThat(finalData.getIceCream())
        .contains("Chocolate");
    assertThat(new BusinessDataMemory().getMessages(initData.getAiMemoryId()))
        .as("Memory should be evicted after human resolution")
        .isEmpty();
    assertThat(finalData.getAiMemoryId())
        .as("Memory id should be evicted after human resolution")
        .isEmpty();
  }

  private Response humanInTheLoopChat(JsonNode req) {
    System.out.println(req.toPrettyString());
    JsonNode messages = req.get("messages");
    if (messages.size() == 2) {
      return responder.send("humanTool-r1.json");
    }
    var humanToolResult = messages.get(3);
    assertThat(humanToolResult.get("role").asText())
        .isEqualTo("tool");
    assertThat(humanToolResult.get("content").asText())
        .isEqualTo("Chocolate");
    return responder.send("humanTool-r2.json");
  }

}
