package com.axonivy.utils.smart.workflow.permission;

public enum SmartWorkflowPermission {
    
    ADMIN("AiAdmin");
    
    private final String roleName;

    SmartWorkflowPermission(String roleName) {
        this.roleName = roleName;
    }

    public String getRoleName() {
        return roleName;
    }
}
