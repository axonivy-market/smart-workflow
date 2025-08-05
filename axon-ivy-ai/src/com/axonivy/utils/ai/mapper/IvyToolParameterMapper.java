package com.axonivy.utils.ai.mapper;

import java.util.function.Function;

import com.axonivy.utils.ai.dto.IvyToolParameter;

import ch.ivyteam.ivy.process.model.value.scripting.VariableDesc;

public class IvyToolParameterMapper {

  @SuppressWarnings("restriction")
  public static final Function<VariableDesc, IvyToolParameter> fromVariableDesc = variableDesc -> {
    IvyToolParameter param = new IvyToolParameter();
    param.setDefinition(variableDesc);
    param.setValue(null);
    return param;
  };
}
