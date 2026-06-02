package com.axonivy.utils.smart.workflow.demo.erp.procurement.model;

import java.io.Serializable;
import java.util.UUID;

public class MaterialType implements Serializable {

  private static final long serialVersionUID = 1L;

  private String id;
  private String name;
  private String description;
  private boolean active;

  public MaterialType() {
    this.id = UUID.randomUUID().toString();
    this.active = true;
  }

  public MaterialType(String name, String description) {
    this();
    this.name = name;
    this.description = description;
  }

  public String getId() { return id; }
  public void setId(String id) { this.id = id; }

  public String getName() { return name; }
  public void setName(String name) { this.name = name; }

  public String getDescription() { return description; }
  public void setDescription(String description) { this.description = description; }

  public boolean isActive() { return active; }
  public void setActive(boolean active) { this.active = active; }
}
