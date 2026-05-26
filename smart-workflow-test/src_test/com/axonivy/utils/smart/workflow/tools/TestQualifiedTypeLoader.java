package com.axonivy.utils.smart.workflow.tools;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.ParameterizedType;

import org.junit.jupiter.api.Test;

import com.axonivy.utils.smart.workflow.spi.internal.ProjectClassLoader;
import com.axonivy.utils.smart.workflow.test.Person;
import com.axonivy.utils.smart.workflow.tools.internal.QualifiedTypeLoader;
import com.axonivy.utils.smart.workflow.tools.internal.QualifiedTypeLoader.QType;

import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
class TestQualifiedTypeLoader {

  @Test
  void loadJavaClass() throws Exception {
    var type = loader().load(new QType(String.class.getName()));
    assertThat(type).isEqualTo(String.class);
  }

  @Test
  void loadJavaPrimitive() throws Exception {
    assertThat(loader().load(new QType("int"))).isEqualTo(int.class);
    assertThat(loader().load(new QType("boolean"))).isEqualTo(boolean.class);
    assertThat(loader().load(new QType("double"))).isEqualTo(double.class);
  }

  @Test
  void loadList() throws Exception {
    var type = loader().load(new QType("java.util.List<java.lang.String>"));
    assertThat(type).isInstanceOf(ParameterizedType.class);
    assertThat(type.getTypeName()).isEqualTo("java.util.List<java.lang.String>");
  }

  @Test
  void loadWithExternalClassLoader_explicit() throws Exception {
    var cl = Person.class.getClassLoader();
    var type = new QualifiedTypeLoader(cl).load(new QType(Person.class.getName()));
    assertThat(type).isEqualTo(Person.class);
  }

  @Test
  void loadWithExternalClassLoader_pmvContext() throws Exception {
    var cl =  ProjectClassLoader.current();
    var type = new QualifiedTypeLoader(cl).load(new QType(Person.class.getName()));
    assertThat(type).isEqualTo(Person.class);
  }

  @Test
  void loadIncorrectType() throws Exception {
    assertThat(loader().load(null)).isNull();
    assertThat(loader().load(new QType(""))).isNull();
    assertThatThrownBy(() -> loader().load(new QType("com.example.DoesNotExist")))
        .isInstanceOf(ClassNotFoundException.class);
  }

  private static QualifiedTypeLoader loader() {
    return new QualifiedTypeLoader(ProjectClassLoader.current());
  }

}
