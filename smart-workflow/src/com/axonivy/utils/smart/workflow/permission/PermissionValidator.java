package com.axonivy.utils.smart.workflow.permission;

import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.security.IRole;

public final class PermissionValidator {
    public static boolean isAiAdmin() {
        return Ivy.session().getSessionUser().getAllRoles().stream()
            .map(IRole::getName)
            .anyMatch(roleName -> roleName.equals(SmartWorkflowPermission.ADMIN.getRoleName()));
    }
}
