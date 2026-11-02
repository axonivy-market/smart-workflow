package com.axonivy.utils.smart.workflow.components.chatbot.service;

import java.util.Map;

import ch.ivyteam.ivy.process.call.SubProcessCallStartEvent;
import ch.ivyteam.ivy.process.call.SubProcessCallStartParam;
import ch.ivyteam.ivy.process.call.SubProcessCallStartParamCaller;
import ch.ivyteam.ivy.process.call.SubProcessSearchFilter;
import ch.ivyteam.ivy.process.call.SubProcessSearchFilter.SearchScope;
import ch.ivyteam.ivy.security.exec.Sudo;

public final class IvyAdapterService {

  private IvyAdapterService() {
  }

  public static Map<String, Object> startSubProcessInSecurityContext(String signature, Map<String, Object> params) {
    Map<String, Object> processParams = params != null ? params : Map.of();
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

      SubProcessCallStartEvent processStart = processStarts.get(0);
      if (processParams.isEmpty()) {
        return processStart.call().asMap();
      }
      // named params must be called through the caller returned by withParam;
      // processStart.call() is the positional varargs variant and rejects them
      SubProcessCallStartParam caller = processStart;
      for (Map.Entry<String, Object> param : processParams.entrySet()) {
        caller = caller.withParam(param.getKey(), param.getValue());
      }
      return ((SubProcessCallStartParamCaller) caller).call().asMap();
    });
  }
}
