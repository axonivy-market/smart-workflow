package com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.enums;


public enum AuditActorType {
  USER  ("so-tl-bubble-user",    "ti-user",     "so-badge-blue",   "User"),
  AGENT ("so-tl-bubble-agent",   "ti-robot",    "so-badge-purple", "Agent"),
  SYSTEM("so-tl-bubble-pending", "ti-settings", "so-badge-gray",   "System");

  public final String bubbleClass;
  public final String icon;
  public final String badgeClass;
  public final String roleLabel;

  AuditActorType(String bubbleClass, String icon, String badgeClass, String roleLabel) {
    this.bubbleClass = bubbleClass;
    this.icon        = icon;
    this.badgeClass  = badgeClass;
    this.roleLabel   = roleLabel;
  }

  public String getBubbleClass() { return bubbleClass; }
  public String getIcon()        { return icon; }
  public String getBadgeClass()  { return badgeClass; }
  public String getRoleLabel()   { return roleLabel; }
}
