package com.axonivy.utils.smart.orchestrator.tools.subprocess;

import static ch.ivyteam.ivy.process.model.value.scripting.VariableDesc.var;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.axonivy.utils.smart.orchestrator.test.Person;
import com.axonivy.utils.smart.orchestrator.tools.internal.JsonToolParamBuilder;

import ch.ivyteam.ivy.application.IProcessModelVersion;
import ch.ivyteam.ivy.environment.IvyTest;
import ch.ivyteam.ivy.process.model.value.scripting.VariableDesc;
import ch.ivyteam.ivy.process.model.value.scripting.VariableInfo;
import dev.langchain4j.model.chat.request.json.JsonArraySchema;
import dev.langchain4j.model.chat.request.json.JsonIntegerSchema;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.request.json.JsonStringSchema;

@IvyTest
@SuppressWarnings("restriction")
class TestIvySubProcessToolSpecs {

  @Test
  void complexParams() {
    List<VariableDesc> userVars = List.of(
        var("id", "Integer").setInfo(new VariableInfo("the user id")),
        var("person", Person.class.getName()));
    var params = paramsOf(userVars);

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
    List<VariableDesc> userVars = List.of(var("users", "List<" + Person.class.getName() + ">"));
    var params = paramsOf(userVars);

    assertThat(params.properties().keySet()).containsOnly("users");
    var users = (JsonArraySchema) params.properties().get("users");

    var item = users.items();
    assertThat(item)
        .isInstanceOf(JsonObjectSchema.class);
    var person = (JsonObjectSchema) item;
    assertThat(person.properties().keySet())
        .containsOnly("firstName", "lastName");
  }

  private static JsonObjectSchema paramsOf(List<VariableDesc> userVars) {
    return new JsonToolParamBuilder(IProcessModelVersion.current()).toParams(userVars);
  }

}
