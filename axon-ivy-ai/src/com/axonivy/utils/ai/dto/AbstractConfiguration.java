package com.axonivy.utils.ai.dto;

import com.axonivy.utils.ai.utils.IdGenerationUtils;
import com.fasterxml.jackson.annotation.JsonIgnore;

public abstract class AbstractConfiguration {
  private String id;
  private String version;

  @JsonIgnore
  private boolean isPublic;

  public AbstractConfiguration() {
    id = IdGenerationUtils.generateRandomId();
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public boolean getIsPublic() {
    return isPublic;
  }

  public void setIsPublic(boolean isPublic) {
    this.isPublic = isPublic;
  }

}