package com.axonivy.utils.smart.orchestrator.demo.enums;


public enum AxonIvyTicketType {
  PORTAL("PortalSupport"), CORE("CoreSupport"), MARKET("MarketSupport");

  private AxonIvyTicketType(String roleName) {
    this.roleName = roleName;
  }

  private String roleName;

  public String getRoleName() {
    return this.roleName;
  }
}
