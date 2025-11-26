package com.axonivy.utils.smart.workflow.tools.internal;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.google.inject.util.Types;

/**
 * Forked from ivy-core internals.
 * If this is useful somewhere else, leave me a note
 * and we consider introduce a PublicAPI for it.
 */
public class QualifiedTypeLoader {

  public record QType(String fqName) {

    public boolean isGeneric() {
      return fqName.contains("<") && fqName.endsWith(">");
    }

    public boolean isEmpty() {
      return fqName.isBlank();
    }

    public String rawType() {
      return StringUtils.substringBefore(fqName, "<");
    }

    public List<QType> getParameters() {
      if (!isGeneric()) {
        return List.of();
      }

      String parameterTypeNamesRaw = fqName.substring(fqName.indexOf("<") + 1, fqName.length() - 1);
      List<QType> params = new ArrayList<>();
      int last = 0;
      int open = 0;
      for (int i = 0; i < parameterTypeNamesRaw.length(); i++) {
        char charAt = parameterTypeNamesRaw.charAt(i);
        if (charAt == '<') {
          open++;
        } else if (charAt == '>') {
          open--;
        } else if (charAt == ',' && open == 0) {
          params.add(new QType(parameterTypeNamesRaw.substring(last, i)));
          last = i + 1;
        }
      }
      params.add(new QType(parameterTypeNamesRaw.substring(last)));
      return params;
    }

  }

  public Type load(QType qType) throws ClassNotFoundException, IllegalStateException {
    if (qType == null || qType.isEmpty()) {
      return null;
    }

    Class<?> rawType = loadRaw(qType);
    if (!qType.isGeneric()) {
      return rawType;
    }

    Type[] typeArgs = loadParameters(qType);
    return Types.newParameterizedType(rawType, typeArgs);
  }

  private Class<?> loadRaw(QType type) throws IllegalStateException, ClassNotFoundException {
    return Class.forName(type.rawType());
  }

  private Type[] loadParameters(QType qType) throws ClassNotFoundException {
    List<Type> paramTypes = new ArrayList<>();
    for (QType qParamType : qType.getParameters()) {
      paramTypes.add(load(qParamType));
    }
    return paramTypes.toArray(new Type[paramTypes.size()]);
  }
}
