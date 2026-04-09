package com.axonivy.utils.smart.workflow.tools.subprocess;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.axonivy.utils.smart.workflow.test.Person;
import com.axonivy.utils.smart.workflow.tools.internal.JsonProcessParameters;
import com.axonivy.utils.smart.workflow.tools.provider.ToolParameter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
class TestJsonProcessParameters {

  @Test
  void complexParams() {
    var params = List.of(
        ToolParameter.of("id", "the user id", Integer.class.getName()),
        ToolParameter.of("person", null, Person.class.getName()));

    var jPayload = JsonNodeFactory.instance.objectNode();
    jPayload.put("id", 123);
    jPayload.putObject("person").put("firstName", "Henry").put("lastName", "Ford");

    var result = paramsOf(params, jPayload);
    assertThat(result.get("id")).isEqualTo(123);
    var person = (Person) result.get("person");
    assertThat(person.getFirstName()).isEqualTo("Henry");
    assertThat(person.getLastName()).isEqualTo("Ford");
  }

  @Test
  void missingParamReturnsNull() {
    var params = List.of(ToolParameter.of("name", null, String.class.getName()));
    var jPayload = JsonNodeFactory.instance.objectNode();

    var result = paramsOf(params, jPayload);
    assertThat(result.get("name")).isNull();
  }

  @Test
  void emptyParams() {
    var result = new JsonProcessParameters().readParams(List.of(), "{}");
    assertThat(result).isEmpty();
  }

  private static Map<String, Object> paramsOf(List<ToolParameter> params, JsonNode payload) {
    return new JsonProcessParameters().toParams(params, payload);
  }
}
