package com.axonivy.utils.smart.workflow.tools.subprocess;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.axonivy.utils.smart.workflow.test.Person;
import com.axonivy.utils.smart.workflow.tools.internal.JsonToolParamBuilder;
import com.axonivy.utils.smart.workflow.tools.provider.ToolParameter;

import ch.ivyteam.ivy.environment.IvyTest;
import dev.langchain4j.model.chat.request.json.JsonArraySchema;
import dev.langchain4j.model.chat.request.json.JsonIntegerSchema;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.request.json.JsonStringSchema;

@IvyTest
class TestIvySubProcessToolSpecs {

  @Test
  void complexParams() {
    var params = paramsOf(List.of(
        ToolParameter.of("id", "the user id", Integer.class.getName()),
        ToolParameter.of("person", null, Person.class.getName())));

    assertThat(params.properties().keySet())
        .containsExactly("id", "person");

    var id = params.properties().get("id");
    assertThat(id)
        .isInstanceOf(JsonIntegerSchema.class);
    assertThat(id.description())
        .isEqualTo("the user id");

    var person = (JsonObjectSchema) params.properties().get("person");
    assertThat(person.properties().keySet())
        .containsExactly("firstName", "lastName");
    assertThat(person.properties().get("firstName"))
        .isInstanceOf(JsonStringSchema.class);
  }

  @Test
  void collectionParams() {
    var params = paramsOf(List.of(
        ToolParameter.of("users", null, "java.util.List<" + Person.class.getName() + ">")));

    assertThat(params.properties().keySet()).containsOnly("users");
    var users = (JsonArraySchema) params.properties().get("users");
    assertThat(users.items()).isInstanceOf(JsonObjectSchema.class);
    var person = (JsonObjectSchema) users.items();
    assertThat(person.properties().keySet()).containsOnly("firstName", "lastName");
  }

  @Test
  void zeroParams() {
    assertThat(paramsOf(List.of()))
        .as("no empty properties, description and the like is better accepted by the Arize Phoenix playground")
        .isNull();
  }

  private static JsonObjectSchema paramsOf(List<ToolParameter> params) {
    return new JsonToolParamBuilder().toParams(params);
  }
}
