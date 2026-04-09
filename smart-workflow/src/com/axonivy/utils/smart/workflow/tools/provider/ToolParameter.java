package com.axonivy.utils.smart.workflow.tools.provider;

public record ToolParameter(String name, String description, String type) {

  public static final String STRING = "java.lang.String";
  public static final String NUMBER = "java.lang.Double";
  public static final String INTEGER = "java.lang.Long";
  public static final String BOOLEAN = "java.lang.Boolean";
  public static final String STRING_LIST = "java.util.List<java.lang.String>";
  public static final String NUMBER_LIST = "java.util.List<java.lang.Double>";

  public static ToolParameter of(String name, String description, String type) {
    return new ToolParameter(name, description, type);
  }
}
