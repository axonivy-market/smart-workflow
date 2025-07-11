package com.axonivy.utils.ai.exception.ai;

import java.util.List;

public class OpenAIErrorResponse {
  private OpenAIError error;

  public OpenAIError getError() {
    return error;
  }

  public void setError(OpenAIError error) {
    this.error = error;
  }

  public class OpenAIError {
    private String message;
    private String type;
    private List<String> param;
    private String code;

    public String getMessage() {
      return message;
    }

    public void setMessage(String message) {
      this.message = message;
    }

    public String getType() {
      return type;
    }

    public void setType(String type) {
      this.type = type;
    }

    public List<String> getParam() {
      return param;
    }

    public void setParam(List<String> param) {
      this.param = param;
    }

    public String getCode() {
      return code;
    }

    public void setCode(String code) {
      this.code = code;
    }
  }
}