package com.axonivy.utils.smart.workflow.governance.service.internal;

import org.apache.commons.lang3.StringUtils;

import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.security.exec.Sudo;
import ch.ivyteam.ivy.workflow.ICase;

public class CaseService {

  private static final String DISPLAY_NAME_FORMAT = "%s (%s)";
  private static final String NO_NAME_FORMAT = "Case: %s";
  private CaseService() {}

  public static String getDisplayName(String caseUuid) {
    if (StringUtils.isBlank(caseUuid)) {
      return String.format(NO_NAME_FORMAT, caseUuid);
    }
    try {
      ICase ivyCase = Sudo.get(() -> Ivy.wf().findCase(caseUuid));
      if (ivyCase == null) {
        return String.format(NO_NAME_FORMAT, caseUuid);
      }
      Ivy.log().error(String.format("Found case with uuid: %s and id: %s and name: %s", caseUuid, ivyCase.getId(), ivyCase.getName()));
      String name = ivyCase.getName();
      return StringUtils.isNotBlank(name) ?
        String.format(DISPLAY_NAME_FORMAT, name, ivyCase.getId()) : Long.toString(ivyCase.getId());
    } catch (Exception e) {
      return String.format(NO_NAME_FORMAT, caseUuid);
    }
  }

  public static ICase findCase(String caseUuid) {
    try {
      return Sudo.get(() -> Ivy.wf().findCase(caseUuid));
    } catch (Exception e) {
      return null;
    }
  }

  public static boolean matchesSearch(String caseUuid, String term) {
    if (StringUtils.isBlank(term)) {
      return true;
    }
    try {
      ICase ivyCase = Sudo.get(() -> Ivy.wf().findCase(caseUuid));
      if (ivyCase == null) {
        return false;
      }
      if (String.valueOf(ivyCase.getId()).contains(term)) {
        return true;
      }
      if (StringUtils.containsIgnoreCase(ivyCase.getName(), term)) {
        return true;
      }
      if (StringUtils.containsIgnoreCase(ivyCase.getDescription(), term)) {
        return true;
      }
      return false;
    } catch (Exception e) {
      return false;
    }
  }
}
