package com.axonivy.utils.smart.workflow.tools.subprocess;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.axonivy.utils.smart.workflow.test.Person;
import com.axonivy.utils.smart.workflow.tools.internal.JsonProcessParameters;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import ch.ivyteam.ivy.environment.IvyTest;
import ch.ivyteam.ivy.process.call.StartParameter;
import ch.ivyteam.ivy.process.call.impl.DefaultStartParameter;

@IvyTest
@SuppressWarnings("restriction")
class TestJsonProcessParameters {

  @Test
  void complexParams() {
    List<StartParameter> userVars = List.of(
        new DefaultStartParameter("id", Integer.class.getName(), "the user id"),
        new DefaultStartParameter("person", Person.class.getName(), null));

    var jPayload = JsonNodeFactory.instance.objectNode();
    jPayload.put("id", 123);
    ObjectNode jPerson = jPayload.putObject("person");
    jPerson.put("firstName", "Henry");
    jPerson.put("lastName", "Ford");

    var params = paramsOf(userVars, jPayload);
    assertThat(params.get("id"))
        .isEqualTo(123);
    var person = (Person) params.get("person");
    assertThat(person.getFirstName())
        .isEqualTo("Henry");
    assertThat(person.getLastName())
        .isEqualTo("Ford");
  }

  private static Map<String, Object> paramsOf(List<StartParameter> userVars, JsonNode payload) {
    return new JsonProcessParameters().toParams(userVars, payload);
  }

}
