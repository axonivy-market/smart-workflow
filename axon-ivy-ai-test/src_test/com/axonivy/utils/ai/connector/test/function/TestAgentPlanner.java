package com.axonivy.utils.ai.connector.test.function;

import static ch.ivyteam.test.client.OpenAiTestClient.structuredOutputAiMock;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.axonivy.utils.ai.connector.OpenAiServiceConnector;
import com.axonivy.utils.ai.connector.OpenAiServiceConnector.OpenAiConf;
import com.axonivy.utils.ai.core.AiStep;
import com.axonivy.utils.ai.core.tool.IvyTool;
import com.axonivy.utils.ai.function.AgentPlanner;

import ch.ivyteam.ivy.environment.AppFixture;
import ch.ivyteam.ivy.environment.IvyTest;
import ch.ivyteam.test.client.OpenAiTestClient;
import ch.ivyteam.test.log.LoggerAccess;
import dev.langchain4j.http.client.log.LoggingHttpClient;

@IvyTest(enableWebServer = true)
public class TestAgentPlanner {

  private OpenAiServiceConnector connector;

  @RegisterExtension
  LoggerAccess log = new LoggerAccess(LoggingHttpClient.class.getName());

  @BeforeEach
  void setup(AppFixture fixture) {
    fixture.var(OpenAiConf.BASE_URL, OpenAiTestClient.localMockApiUrl());
    fixture.var(OpenAiConf.TEST_HEADER, "agentplanner");

    connector = new OpenAiServiceConnector();
    connector.setJsonModel(structuredOutputAiMock());
  }

  @Test
  void testSimplePlanGeneration() {
    // Given
    List<IvyTool> tools = createSimpleTools();

    AgentPlanner planner = AgentPlanner.getBuilder().useService(connector)
        .withQuery("Process a customer support ticket").addTools(tools).build();

    // When
    List<AiStep> result = planner.execute();

    // Then
    assertThat(result).isNotNull();
    assertThat(result).hasSize(2);

    AiStep firstStep = result.get(0);
    assertThat(firstStep.getStepNo()).isEqualTo(1);
    assertThat(firstStep.getName()).isEqualTo("Validate Ticket");
    assertThat(firstStep.getToolId()).isEqualTo("TICKET_VALIDATOR");
    assertThat(firstStep.getPrevious()).isEqualTo(0);
    assertThat(firstStep.getNext()).isEqualTo(2);

    AiStep secondStep = result.get(1);
    assertThat(secondStep.getStepNo()).isEqualTo(2);
    assertThat(secondStep.getName()).isEqualTo("Send Response");
    assertThat(secondStep.getToolId()).isEqualTo("EMAIL_SENDER");
    assertThat(secondStep.getPrevious()).isEqualTo(1);
    assertThat(secondStep.getNext()).isEqualTo(-1);
  }

  @Test
  void testComplexWorkflowPlanning() {
    // Given
    List<IvyTool> tools = createWorkflowTools();

    AgentPlanner planner = AgentPlanner.getBuilder().useService(connector)
        .withQuery("Complete customer onboarding process").addTools(tools).build();

    // When
    List<AiStep> result = planner.execute();

    // Then
    assertThat(result).isNotNull();
    assertThat(result).hasSize(3);

    // Verify step sequence
    assertThat(result.get(0).getStepNo()).isEqualTo(1);
    assertThat(result.get(0).getToolId()).isEqualTo("USER_CREATOR");
    assertThat(result.get(1).getStepNo()).isEqualTo(2);
    assertThat(result.get(1).getToolId()).isEqualTo("DOCUMENT_PROCESSOR");
    assertThat(result.get(2).getStepNo()).isEqualTo(3);
    assertThat(result.get(2).getToolId()).isEqualTo("NOTIFICATION_SENDER");

    // Verify workflow connections
    assertThat(result.get(0).getNext()).isEqualTo(2);
    assertThat(result.get(1).getPrevious()).isEqualTo(1);
    assertThat(result.get(2).getNext()).isEqualTo(-1);
  }

  @Test
  void testPlanningWithCustomInstructions() {
    // Given
    List<IvyTool> tools = createSimpleTools();

    AgentPlanner planner = AgentPlanner.getBuilder().useService(connector).withQuery("Handle urgent customer complaint")
        .addTools(tools).addCustomInstruction("Prioritize speed and customer satisfaction")
        .addCustomInstruction("Include escalation step if needed").build();

    // When
    List<AiStep> result = planner.execute();

    // Then
    assertThat(result).isNotNull();
    assertThat(result).hasSize(3);

    // Should include escalation step due to custom instructions
    assertThat(result.stream().anyMatch(step -> step.getAnalysis().toLowerCase().contains("escalation")
        || step.getName().toLowerCase().contains("escalation"))).isTrue();
  }

  @Test
  void testPlanGenerationWithSingleTool() {
    // Given
    List<IvyTool> tools = List.of(createTool("DATA_ANALYZER", "Analyze data and generate reports"));

    AgentPlanner planner = AgentPlanner.getBuilder().useService(connector).withQuery("Generate monthly sales report")
        .addTools(tools).build();

    // When
    List<AiStep> result = planner.execute();

    // Then
    assertThat(result).isNotNull();
    assertThat(result).hasSize(1);

    AiStep step = result.get(0);
    assertThat(step.getToolId()).isEqualTo("DATA_ANALYZER");
    assertThat(step.getStepNo()).isEqualTo(1);
    assertThat(step.getPrevious()).isEqualTo(0);
    assertThat(step.getNext()).isEqualTo(-1);
  }

  @Test
  void testErrorHandlingForInvalidResponse() {
    // Given
    List<IvyTool> tools = createSimpleTools();

    AgentPlanner planner = AgentPlanner.getBuilder().useService(connector).withQuery("invalid_json_response")
        .addTools(tools).build();

    // When
    List<AiStep> result = planner.execute();

    // Then
    assertThat(result).isNotNull();
    assertThat(result).isEmpty();
  }

  @Test
  void testErrorHandlingForEmptyTools() {
    // Given
    AgentPlanner planner = AgentPlanner.getBuilder().useService(connector).withQuery("Some task")
        .addTools(new ArrayList<>()).build();

    // When
    List<AiStep> result = planner.execute();

    // Then
    assertThat(result).isNull(); // Should fail validation
  }

  @Test
  void testBuilderPattern() {
    // Given
    List<IvyTool> tools = createSimpleTools();
    IvyTool additionalTool = createTool("EXTRA_TOOL", "Additional functionality");

    // When
    AgentPlanner planner = AgentPlanner.getBuilder().useService(connector).withQuery("test query").addTools(tools)
        .addTool(additionalTool).addCustomInstruction("test instruction").build();

    // Then
    assertThat(planner).isNotNull();
    assertThat(planner.getQuery()).isEqualTo("test query");
    assertThat(planner.getTools()).hasSize(3); // 2 from list + 1 additional
    assertThat(planner.getCustomInstructions()).contains("test instruction");
  }

  @Test
  void testBuilderAddSingleTool() {
    // Given
    IvyTool tool = createTool("SINGLE_TOOL", "Single tool functionality");

    // When
    AgentPlanner planner = AgentPlanner.getBuilder().useService(connector).withQuery("test query").addTool(tool)
        .build();

    // Then
    assertThat(planner).isNotNull();
    assertThat(planner.getTools()).hasSize(1);
    assertThat(planner.getTools().get(0).getId()).isEqualTo("SINGLE_TOOL");
  }

  @Test
  void testGetStepsMethod() {
    // Given
    List<IvyTool> tools = createSimpleTools();

    AgentPlanner planner = AgentPlanner.getBuilder().useService(connector).withQuery("Test steps method")
        .addTools(tools).build();

    // When
    List<AiStep> result = planner.execute();
    List<AiStep> steps = planner.getSteps();

    // Then
    assertThat(steps).isEqualTo(result);
    assertThat(steps).isNotNull();
  }

  @Test
  void requestLogAccess() {
    structuredOutputAiMock().chat("ready?");

    assertThat(httpRequestLog()).as("transport logs are easy to access and assert in tests").contains("url: http://")
        .contains("/api/aiMock/chat/completions");
  }

  private String httpRequestLog() {
    return log.infos().stream().filter(line -> line.startsWith("HTTP request")).findFirst().orElseThrow();
  }

  // Helper methods to create test data
  private List<IvyTool> createSimpleTools() {
    List<IvyTool> tools = new ArrayList<>();
    tools.add(createTool("TICKET_VALIDATOR", "Validate incoming support tickets"));
    tools.add(createTool("EMAIL_SENDER", "Send automated email responses"));
    return tools;
  }

  private List<IvyTool> createWorkflowTools() {
    List<IvyTool> tools = new ArrayList<>();
    tools.add(createTool("USER_CREATOR", "Create new user accounts"));
    tools.add(createTool("DOCUMENT_PROCESSOR", "Process and store customer documents"));
    tools.add(createTool("NOTIFICATION_SENDER", "Send welcome notifications"));
    return tools;
  }

  private IvyTool createTool(String id, String usage) {
    IvyTool tool = new IvyTool();
    tool.setId(id);
    tool.setUsage(usage);
    tool.setName(id.toLowerCase().replace("_", " "));
    return tool;
  }
}