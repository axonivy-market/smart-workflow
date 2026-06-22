package com.axonivy.utils.smart.workflow.demo.supplier.onboarding.bean.interfaces;

import javax.el.ELContext;
import javax.el.MethodExpression;
import javax.faces.application.Application;
import javax.faces.context.FacesContext;

public interface LogicCloseSupport {

  String LOGIC_CLOSE_EL = "#{logic.close}";

  default void callLogicClose(Object argument) {
    FacesContext fc = FacesContext.getCurrentInstance();
    ELContext el = fc.getELContext();
    Application app = fc.getApplication();
    MethodExpression closeMethod = app.getExpressionFactory()
        .createMethodExpression(el, LOGIC_CLOSE_EL, null, new Class<?>[] { Object.class });
    closeMethod.invoke(el, new Object[] { argument });
  }
}
