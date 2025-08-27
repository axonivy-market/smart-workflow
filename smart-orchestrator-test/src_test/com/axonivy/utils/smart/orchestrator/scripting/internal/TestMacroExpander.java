package com.axonivy.utils.smart.orchestrator.scripting.internal;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ch.ivyteam.ivy.application.IProcessModelVersion;
import ch.ivyteam.ivy.environment.IvyTest;
import ch.ivyteam.ivy.scripting.dataclass.IProjectDataClassManager;
import ch.ivyteam.ivy.scripting.exceptions.IvyScriptException;
import ch.ivyteam.ivy.scripting.language.IIvyScriptContext;
import ch.ivyteam.ivy.scripting.objects.CompositeObject;
import ch.ivyteam.ivy.security.ISecurity;
import ch.ivyteam.security.Password;
import dev.langchain4j.internal.Json;

@IvyTest
public class TestMacroExpander {

  private IIvyScriptContext context;
  private MyData in;

  @Test
  void expand() {
    var expanded = new MacroExpander(context).expand("hey <%=ivy.session.getSessionUserName()%>");
    assertThat(expanded.get()).isEqualTo("hey Junit");
  }

  @Test
  void expand_objectToJson() {
    var expanded = new MacroExpander(context).expand("hey <%=in%>");
    String json = Json.toJson(in);
    assertThat(expanded.get()).isEqualTo("hey " + json);
  }

  @BeforeEach
  void setUp() throws IvyScriptException {
    var repo = IProjectDataClassManager.of(IProcessModelVersion.current().project()).getIvyScriptClassRepository();
    var testSession = ISecurity.current().sessions().create("tester");
    testSession.authenticateSessionUser("Junit", new Password("junit"));
    this.in = new MyData();
    this.context = IIvyScriptContext.create(repo)
        .ivyVariableMember("session", testSession)
        .variable("in", in)
        .toIvyScriptContext();
  }

  public static class MyData extends CompositeObject {
    public String userName = "Fritz";
    public String lastName = "Frosch";
  }

}
