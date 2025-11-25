package com.axonivy.utils.smart.workflow.guardrails;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

public class GuardrailResult {

  public static enum ResultStatus {
    SUCCESS, FAIL;
  }

  private String resultMessage;
  private List<String> errors;
  private ResultStatus status;

  public String getResultMessage() {
    return resultMessage;
  }

  public void setResultMessage(String resultMessage) {
    this.resultMessage = resultMessage;
  }

  public List<String> getErrors() {
    return errors;
  }

  public void setErrors(List<String> errors) {
    this.errors = errors;
  }

  public ResultStatus getStatus() {
    return status;
  }

  public void setStatus(ResultStatus status) {
    this.status = status;
  }

  public static GuardrailResult success(String resultMessage) {
    GuardrailResult result = new GuardrailResult();
    result.setErrors(null);
    result.setResultMessage(resultMessage);
    result.setStatus(ResultStatus.SUCCESS);
    return result;
  }

  public static GuardrailResult fail(String message, List<String> errors) {
    GuardrailResult result = new GuardrailResult();
    result.setResultMessage(StringUtils.EMPTY);
    result.setErrors(errors);
    result.setStatus(ResultStatus.FAIL);
    return result;
  }

  public boolean isSuccess() {
    return ResultStatus.SUCCESS.equals(this.getStatus());
  }
}
