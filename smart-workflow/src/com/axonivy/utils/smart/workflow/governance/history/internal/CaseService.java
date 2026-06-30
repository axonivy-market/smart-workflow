package com.axonivy.utils.smart.workflow.governance.history.internal;

import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.security.exec.Sudo;
import ch.ivyteam.ivy.workflow.ICase;

public class CaseService {

  private static final String DISPLAY_NAME_FORMAT = "%s (%s)";
  private static final String NO_NAME_FORMAT = "Case: %s";

  private CaseService() {}

  public static ICase findCase(String caseUuid) {
    try {
      return Sudo.get(() -> Ivy.wf().findCase(caseUuid));
    } catch (Exception e) {
      return null;
    }
  }

  public static String getDisplayName(String caseUuid) {
    ICase ivyCase = findCase(caseUuid);
    if (ivyCase == null) {
      return String.format(NO_NAME_FORMAT, caseUuid);
    }
    String name = ivyCase.getName();
    return (name == null || name.isBlank()) ?
        String.format(NO_NAME_FORMAT, ivyCase.getId())
        : String.format(DISPLAY_NAME_FORMAT, name, ivyCase.getId());
  }

  public static boolean matchesSearch(String caseUuid, String term) {
    if (term == null || term.isBlank()) {
      return true;
    }
    ICase ivyCase = findCase(caseUuid);
    if (ivyCase == null) {
      return false;
    }
    return String.valueOf(ivyCase.getId()).contains(term)
        || (ivyCase.getName() != null && ivyCase.getName().toLowerCase().contains(term.toLowerCase()))
        || (ivyCase.getDescription() != null && ivyCase.getDescription().toLowerCase().contains(term.toLowerCase()));
  }
}