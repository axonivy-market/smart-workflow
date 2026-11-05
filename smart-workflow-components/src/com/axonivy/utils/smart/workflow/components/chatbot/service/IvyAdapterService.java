package com.axonivy.utils.smart.workflow.components.chatbot.service;

import java.util.Map;

import ch.ivyteam.ivy.process.call.SubProcessCallStartEvent;
import ch.ivyteam.ivy.process.call.SubProcessSearchFilter;
import ch.ivyteam.ivy.process.call.SubProcessSearchFilter.SearchScope;
import ch.ivyteam.ivy.security.exec.Sudo;

public final class IvyAdapterService {

  private IvyAdapterService() {
  }

  public static Map<String, Object> startSubProcessInSecurityContext(String signature, String paramName, Object paramValue) {
    return Sudo.get(() -> {
      var filter = SubProcessSearchFilter.create()
          .setSearchScope(SearchScope.SECURITY_CONTEXT)
          .setSignature(signature)
          .toFilter();
      var processStarts = SubProcessCallStartEvent.find(filter);
      if (processStarts.size() != 1) {
        throw new IllegalStateException(
            "Expected one subprocess with signature '" + signature + "', found " + processStarts.size() + ".");
      }
      return processStarts.get(0).withParam(paramName, paramValue).call().asMap();
    });
  }
}
