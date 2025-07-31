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
import com.axonivy.utils.ai.dto.ai.AiVariable;
import com.axonivy.utils.ai.dto.ai.FieldExplanation;
import com.axonivy.utils.ai.enums.AiVariableState;
import com.axonivy.utils.ai.function.DataMapper;

import ch.ivyteam.ivy.environment.AppFixture;
import ch.ivyteam.ivy.environment.IvyTest;
import ch.ivyteam.test.client.OpenAiTestClient;
import ch.ivyteam.test.log.LoggerAccess;
import dev.langchain4j.http.client.log.LoggingHttpClient;

@IvyTest(enableWebServer = true)
public class TestDataMapper {

  private OpenAiServiceConnector connector;
  @RegisterExtension
  LoggerAccess log = new LoggerAccess(LoggingHttpClient.class.getName());

  @BeforeEach
  void setup(AppFixture fixture) {
    fixture.var(OpenAiConf.BASE_URL, OpenAiTestClient.localMockApiUrl());
    fixture.var(OpenAiConf.TEST_HEADER, "datamapper");

    connector = new OpenAiServiceConnector();
    connector.setJsonModel(structuredOutputAiMock());
  }

  // Simple test object for data mapping
  public static class TestPerson {
    private String name;
    private Integer age;
    private Boolean isActive;
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
  }

  // Test object with enum
  public static class TestEmployee {
    private String name;
    private TestPosition position;
    private Integer salary;
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public TestPosition getPosition() { return position; }
    public void setPosition(TestPosition position) { this.position = position; }
    public Integer getSalary() { return salary; }
    public void setSalary(Integer salary) { this.salary = salary; }
  }

  public enum TestPosition {
    JUNIOR, SENIOR, MANAGER
  }

  @Test
  void testDataMappingWithFieldExplanations() {
    // Given
    TestPerson targetObject = new TestPerson();

    List<FieldExplanation> fieldExplanations = new ArrayList<>();
    FieldExplanation nameExplanation = new FieldExplanation("name", "The full name of the person");
    nameExplanation.setMandatory(true);
    fieldExplanations.add(nameExplanation);
    
    FieldExplanation ageExplanation = new FieldExplanation("age", "Age in years, must be positive");
    fieldExplanations.add(ageExplanation);

    DataMapper dataMapper = DataMapper.getBuilder()
        .useService(connector)
        .withQuery("Extract person: Jane Smith, 25")
        .withTargetObject(targetObject)
        .addFieldExplanations(fieldExplanations)
        .build();

    // When
    AiVariable result = dataMapper.execute();

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getState()).isEqualTo(AiVariableState.SUCCESS);
    
    TestPerson person = (TestPerson) result.getParameter().getValue();
    assertThat(person.getName()).isEqualTo("Jane Smith");
    assertThat(person.getAge()).isEqualTo(25);
  }

  @Test
  void testDataMappingWithEnums() {
    // Given
    TestEmployee targetObject = new TestEmployee();

    DataMapper dataMapper = DataMapper.getBuilder()
        .useService(connector)
        .withQuery("Employee data: Alice Johnson, Senior Developer, 75000 salary")
        .withTargetObject(targetObject)
        .build();

    // When
    AiVariable result = dataMapper.execute();

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getState()).isEqualTo(AiVariableState.SUCCESS);
    
    TestEmployee employee = (TestEmployee) result.getParameter().getValue();
    assertThat(employee.getName()).isEqualTo("Alice Johnson");
    assertThat(employee.getPosition()).isEqualTo(TestPosition.SENIOR);
    assertThat(employee.getSalary()).isEqualTo(75000);
  }

  @Test
  void testDataMappingWithCustomInstructions() {
    // Given
    TestPerson targetObject = new TestPerson();

    DataMapper dataMapper = DataMapper.getBuilder()
        .useService(connector)
        .withQuery("Extract: Chris Davis, twenty-five years, working")
        .withTargetObject(targetObject)
        .addCustomInstruction("Convert text numbers to digits")
        .addCustomInstruction("Map 'working' to active status")
        .build();

    // When
    AiVariable result = dataMapper.execute();

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getState()).isEqualTo(AiVariableState.SUCCESS);
    
    TestPerson person = (TestPerson) result.getParameter().getValue();
    assertThat(person.getName()).isEqualTo("Chris Davis");
    assertThat(person.getAge()).isEqualTo(25);
    assertThat(person.getIsActive()).isTrue();
  }

  @Test
  void testErrorHandlingForInvalidJson() {
    // Given
    TestPerson targetObject = new TestPerson();

    DataMapper dataMapper = DataMapper.getBuilder()
        .useService(connector)
        .withQuery("invalid_json_response")
        .withTargetObject(targetObject)
        .build();

    // When
    AiVariable result = dataMapper.execute();

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getState()).isEqualTo(AiVariableState.ERROR);
    assertThat(result.getParameter().getValue()).isEqualTo("ERROR");
  }

  @Test
  void testDataMappingWithNullTargetObject() {
    // Given
    DataMapper dataMapper = DataMapper.getBuilder()
        .useService(connector)
        .withQuery("Some query")
        .withTargetObject(null)
        .build();

    // When
    AiVariable result = dataMapper.execute();

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getState()).isEqualTo(AiVariableState.ERROR);
  }

  @Test
  void testBuilderPattern() {
    // Given
    TestPerson targetObject = new TestPerson();
    
    // When
    DataMapper dataMapper = DataMapper.getBuilder()
        .useService(connector)
        .withQuery("test query")
        .withTargetObject(targetObject)
        .asList(false)
        .build();

    // Then
    assertThat(dataMapper).isNotNull();
    assertThat(dataMapper.getTargetObject()).isEqualTo(targetObject);
    assertThat(dataMapper.getQuery()).isEqualTo("test query");
    assertThat(dataMapper.getAsList()).isFalse();
  }

  @Test
  void testConvertObjectToJson() {
    // Given
    TestPerson person = new TestPerson();
    person.setName("Test Person");
    person.setAge(30);
    person.setIsActive(true);

    DataMapper dataMapper = DataMapper.getBuilder()
        .withTargetObject(person)
        .build();

    // When
    String json = dataMapper.convertObjectToJson();

    // Then
    assertThat(json).isNotEmpty();
    assertThat(json).contains("Test Person");
    assertThat(json).contains("30");
    assertThat(json).contains("true");
  }

  @Test
  void testBuilderWithJsonTarget() {
    // Given
    String jsonTarget = "{\"name\":\"JSON Person\",\"age\":40,\"isActive\":false}";

    // When
    DataMapper dataMapper = DataMapper.getBuilder()
        .withTargetJson(jsonTarget)
        .build();

    // Then
    assertThat(dataMapper).isNotNull();
    assertThat(dataMapper.getTargetObject()).isNotNull();
  }
  
  @Test
  void requestLogAccess() {
    structuredOutputAiMock().chat("ready?");

    assertThat(httpRequestLog())
        .as("transport logs are easy to access and assert in tests").contains("url: http://")
        .contains("/api/aiMock/chat/completions");
  }

  private String httpRequestLog() {
    return log.infos().stream()
        .filter(line -> line.startsWith("HTTP request"))
        .findFirst()
        .orElseThrow();
  }
}
