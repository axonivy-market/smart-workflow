package com.axonivy.utils.ai.axon.ivy.ai.demo.dto;

import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.ai.axon.ivy.ai.demo.enums.TicketType;

public class SupportTicket {
  private String id;
  private TicketType type;
  private String name;
  private String description;
  private String requestor;

  public SupportTicket() {
    setId(UUID.randomUUID().toString().replaceAll("-", StringUtils.EMPTY));
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public TicketType getType() {
    return type;
  }

  public void setType(TicketType type) {
    this.type = type;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getRequestor() {
    return requestor;
  }

  public void setRequestor(String requestor) {
    this.requestor = requestor;
  }
}