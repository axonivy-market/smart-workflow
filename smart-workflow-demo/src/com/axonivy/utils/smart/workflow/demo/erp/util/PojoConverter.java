package com.axonivy.utils.smart.workflow.demo.erp.util;

import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.ConverterException;
import javax.faces.convert.FacesConverter;

@FacesConverter("pojoConverter")
public class PojoConverter implements Converter {
  private static final String UNIQUE_CONVERTER_IDENTIFIER = PojoConverter.class.getName();
  private static final String KEY_DELIMITER = ":::";
  private static final String MAP_KEY_TEMPLATE = UNIQUE_CONVERTER_IDENTIFIER + KEY_DELIMITER + "%s" + KEY_DELIMITER
      + "%s";

  @Override
  public String getAsString(FacesContext context, UIComponent component, Object item) throws ConverterException {
    if (item != null && !isEmptyString(item)) {
      Map<String, Object> viewMap = getViewMap(context);
      String identityHashCode = String.valueOf(System.identityHashCode(item));
      String mapKey = String.format(MAP_KEY_TEMPLATE, component.getId(), identityHashCode);
      viewMap.put(mapKey, item);

      return identityHashCode;
    }
    return "";
  }

  @Override
  public Object getAsObject(FacesContext context, UIComponent component, String selectedvalue) {
    if (selectedvalue != null && selectedvalue.length() > 0) {

      String mapKey = String.format(MAP_KEY_TEMPLATE, component.getId(), selectedvalue);
      Map<String, Object> viewMap = getViewMap(context);

      return viewMap.get(mapKey);
    }
    return null;
  }

  private boolean isEmptyString(Object item) {
    return String.class.isAssignableFrom(item.getClass()) && "".equals(item);
  }

  private Map<String, Object> getViewMap(FacesContext context) {
    UIViewRoot viewRoot = context.getViewRoot();
    return viewRoot.getViewMap();
  }
}