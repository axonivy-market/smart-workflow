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
import com.axonivy.utils.ai.dto.ai.AiOption;
import com.axonivy.utils.ai.dto.ai.AiVariable;
import com.axonivy.utils.ai.enums.AiVariableState;
import com.axonivy.utils.ai.function.DecisionMaker;

import ch.ivyteam.ivy.environment.AppFixture;
import ch.ivyteam.ivy.environment.IvyTest;
import ch.ivyteam.test.client.OpenAiTestClient;
import ch.ivyteam.test.log.LoggerAccess;
import dev.langchain4j.http.client.log.LoggingHttpClient;

@IvyTest(enableWebServer = true)
public class TestDecisionMaker {
  private OpenAiServiceConnector connector;

  @RegisterExtension
  LoggerAccess log = new LoggerAccess(LoggingHttpClient.class.getName());

  @BeforeEach
  void setup(AppFixture fixture) {
    fixture.var(OpenAiConf.BASE_URL, OpenAiTestClient.localMockApiUrl());
    fixture.var(OpenAiConf.TEST_HEADER, "decisionmaker");

    connector = new OpenAiServiceConnector();
    connector.setJsonModel(structuredOutputAiMock());
  }

  @Test
  void testApprovalDecision() {
    // Given
    List<AiOption> options = createApprovalOptions();

    DecisionMaker decisionMaker = DecisionMaker.getBuilder().useService(connector)
        .withQuery("Employee requesting 3 weeks vacation during busy season").addOptions(options).build();

    // When
    AiVariable result = decisionMaker.execute();

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getState()).isEqualTo(AiVariableState.SUCCESS);
    assertThat(result.getParameter().getValue()).isEqualTo("REJECT");
    assertThat(result.getParameter().getClassName()).isEqualTo("String");
  }

  @Test
  void testCategoryDecision() {
    // Given
    List<AiOption> options = createCategoryOptions();

    DecisionMaker decisionMaker = DecisionMaker.getBuilder().useService(connector)
        .withQuery("Customer complaint about incorrect billing charges").addOptions(options).build();

    // When
    AiVariable result = decisionMaker.execute();

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getState()).isEqualTo(AiVariableState.SUCCESS);
    assertThat(result.getParameter().getValue()).isEqualTo("BILLING_ISSUE");
    assertThat(result.getParameter().getClassName()).isEqualTo("String");
  }

  @Test
  void testPriorityDecision() {
    // Given
    List<AiOption> options = createPriorityOptions();

    DecisionMaker decisionMaker = DecisionMaker.getBuilder().useService(connector)
        .withQuery("System is down and customers cannot process payments").addOptions(options).build();

    // When
    AiVariable result = decisionMaker.execute();

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getState()).isEqualTo(AiVariableState.SUCCESS);
    assertThat(result.getParameter().getValue()).isEqualTo("CRITICAL");
    assertThat(result.getParameter().getClassName()).isEqualTo("String");
  }

  @Test
  void testDecisionWithCustomInstructions() {
    // Given
    List<AiOption> options = createApprovalOptions();

    DecisionMaker decisionMaker = DecisionMaker.getBuilder().useService(connector)
        .withQuery("Senior manager requesting 1 week vacation").addOptions(options)
        .addCustomInstruction("Consider employee seniority in decision")
        .addCustomInstruction("Approve requests from management level employees").build();

    // When
    AiVariable result = decisionMaker.execute();

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getState()).isEqualTo(AiVariableState.SUCCESS);
    assertThat(result.getParameter().getValue()).isEqualTo("APPROVE");
  }

  @Test
  void testErrorHandlingForInvalidOptionId() {
    // Given
    List<AiOption> options = createApprovalOptions();

    DecisionMaker decisionMaker = DecisionMaker.getBuilder().useService(connector).withQuery("invalid_option_response")
        .addOptions(options).build();

    // When
    AiVariable result = decisionMaker.execute();

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getState()).isEqualTo(AiVariableState.ERROR);
    assertThat(result.getParameter().getValue()).isEqualTo("ERROR");
  }

  @Test
  void testErrorHandlingForMalformedResponse() {
    // Given
    List<AiOption> options = createApprovalOptions();

    DecisionMaker decisionMaker = DecisionMaker.getBuilder().useService(connector).withQuery("malformed_json_response")
        .addOptions(options).build();

    // When
    AiVariable result = decisionMaker.execute();

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getState()).isEqualTo(AiVariableState.ERROR);
    assertThat(result.getParameter().getValue()).isEqualTo("ERROR");
  }

  @Test
  void testDecisionWithEmptyOptions() {
    // Given
    DecisionMaker decisionMaker = DecisionMaker.getBuilder().useService(connector).withQuery("Any query")
        .addOptions(new ArrayList<>()).build();

    // When
    AiVariable result = decisionMaker.execute();

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getState()).isEqualTo(AiVariableState.ERROR);
  }

  @Test
  void testBuilderPattern() {
    // Given
    List<AiOption> options = createApprovalOptions();

    // When
    DecisionMaker decisionMaker = DecisionMaker.getBuilder().useService(connector).withQuery("test query")
        .addOptions(options).addCustomInstruction("test instruction").build();

    // Then
    assertThat(decisionMaker).isNotNull();
    assertThat(decisionMaker.getQuery()).isEqualTo("test query");
    assertThat(decisionMaker.getOptions()).hasSize(3);
    assertThat(decisionMaker.getCustomInstructions()).contains("test instruction");
  }

  @Test
  void testBuilderAddSingleOption() {
    // Given
    AiOption option = new AiOption("TEST_OPTION", "Test condition");

    // When
    DecisionMaker decisionMaker = DecisionMaker.getBuilder().useService(connector).withQuery("test query")
        .addOption(option).build();

    // Then
    assertThat(decisionMaker).isNotNull();
    assertThat(decisionMaker.getOptions()).hasSize(1);
    assertThat(decisionMaker.getOptions().get(0).getId()).isEqualTo("TEST_OPTION");
  }

  @Test
  void testBuilderWithNullOption() {
    // Given & When
    DecisionMaker decisionMaker = DecisionMaker.getBuilder().useService(connector).withQuery("test query")
        .addOption(null).build();

    // Then
    assertThat(decisionMaker).isNotNull();
    assertThat(decisionMaker.getOptions()).isEmpty();
  }

  @Test
  void testFormatOptions() {
    // Given
    List<AiOption> options = createApprovalOptions();
    DecisionMaker decisionMaker = DecisionMaker.getBuilder().addOptions(options).build();

    // When - The formatOptions method is called internally during execution
    // We can test it indirectly by verifying the options are properly set

    // Then
    assertThat(decisionMaker.getOptions()).hasSize(3);
    assertThat(decisionMaker.getOptions().stream().map(AiOption::getId)).containsExactlyInAnyOrder("APPROVE", "REJECT",
        "ESCALATE");
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
  private List<AiOption> createApprovalOptions() {
    List<AiOption> options = new ArrayList<>();
    options.add(new AiOption("APPROVE", "Request meets all criteria and can be approved"));
    options.add(new AiOption("REJECT", "Request does not meet criteria or timing is inappropriate"));
    options.add(new AiOption("ESCALATE", "Request requires management review and approval"));
    return options;
  }

  private List<AiOption> createCategoryOptions() {
    List<AiOption> options = new ArrayList<>();
    options.add(new AiOption("TECHNICAL_ISSUE", "Technical problem requiring IT support"));
    options.add(new AiOption("BILLING_ISSUE", "Problems with charges, payments, or account billing"));
    options.add(new AiOption("GENERAL_INQUIRY", "General questions or information requests"));
    options.add(new AiOption("COMPLAINT", "Customer dissatisfaction or service complaints"));
    return options;
  }

  private List<AiOption> createPriorityOptions() {
    List<AiOption> options = new ArrayList<>();
    options.add(new AiOption("LOW", "Minor issue that can be addressed in normal timeframe"));
    options.add(new AiOption("MEDIUM", "Important issue requiring prompt attention"));
    options.add(new AiOption("HIGH", "Urgent issue affecting business operations"));
    options.add(new AiOption("CRITICAL", "Emergency situation requiring immediate action"));
    return options;
  }
}
