package com.axonivy.utils.ai.tools.test;

import static ch.ivyteam.ivy.process.model.value.scripting.VariableDesc.var;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.axonivy.utils.ai.core.internal.JsonProcessParameters;
import com.axonivy.utils.ai.test.Person;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import ch.ivyteam.ivy.application.IProcessModelVersion;
import ch.ivyteam.ivy.environment.IvyTest;
import ch.ivyteam.ivy.process.model.value.scripting.VariableDesc;
import ch.ivyteam.ivy.process.model.value.scripting.VariableInfo;

@IvyTest
@SuppressWarnings("restriction")
class TestJsonProcessParameters {

  @Test
  void complexParams() {
    List<VariableDesc> userVars = List.of(
        var("id", "Integer").setInfo(new VariableInfo("the user id")),
        var("person", Person.class.getName()));

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

  private static Map<String, Object> paramsOf(List<VariableDesc> userVars, JsonNode payload) {
    return new JsonProcessParameters(IProcessModelVersion.current()).toParams(userVars, payload);
  }

}
