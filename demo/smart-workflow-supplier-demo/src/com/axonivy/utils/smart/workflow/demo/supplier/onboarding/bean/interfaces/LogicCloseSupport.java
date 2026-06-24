package com.axonivy.utils.smart.workflow.demo.supplier.onboarding.bean.interfaces;

import javax.el.ELContext;
import javax.el.MethodExpression;
import javax.faces.context.FacesContext;


public interface LogicCloseSupport {

  String LOGIC_CLOSE_EL = "#{logic.close}";

  default void close() {
    beforeClose();
    callLogicClose();
  }

  default void close(Object argument) {
    beforeClose();
    callLogicClose(argument);
  }

  default void beforeClose() { }

  default void callLogicClose() {
    invokeLogicClose(new Class<?>[0]);
  }

  default void callLogicClose(Object argument) {
    invokeLogicClose(
        new Class<?>[] { Object.class },
        argument);
  }

  private void invokeLogicClose(Class<?>[] parameterTypes, Object... arguments) {

    FacesContext fc = FacesContext.getCurrentInstance();
    ELContext el = fc.getELContext();

    MethodExpression closeMethod = fc.getApplication()
        .getExpressionFactory()
        .createMethodExpression(
            el,
            LOGIC_CLOSE_EL,
            null,
            parameterTypes);

    closeMethod.invoke(el, arguments);
  }
}