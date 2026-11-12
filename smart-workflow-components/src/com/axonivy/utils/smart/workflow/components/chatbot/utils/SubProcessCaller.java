package com.axonivy.utils.smart.workflow.components.chatbot.utils;

import java.util.Map;

import ch.ivyteam.ivy.process.call.SubProcessCallStartEvent;
import ch.ivyteam.ivy.process.call.SubProcessCallStartParam;
import ch.ivyteam.ivy.process.call.SubProcessCallStartParamCaller;
import ch.ivyteam.ivy.process.call.SubProcessSearchFilter;
import ch.ivyteam.ivy.process.call.SubProcessSearchFilter.SearchScope;
import ch.ivyteam.ivy.security.exec.Sudo;

public final class SubProcessCaller {

  private SubProcessCaller() {
  }

  public static Map<String, Object> call(String signature, Map<String, Object> params) {
    return Sudo.get(() -> {
      var filter = SubProcessSearchFilter.create()
          .setSearchScope(SearchScope.PROJECT_AND_ALL_REQUIRED)
          .setSignature(signature)
          .toFilter();
      var processStarts = SubProcessCallStartEvent.find(filter);
      if (processStarts.size() != 1) {
        throw new IllegalStateException(
            "Expected one subprocess with signature '" + signature + "', found " + processStarts.size() + ".");
      }
      // named params must be chained: each withParam returns the caller that holds the params so far
      SubProcessCallStartParam caller = processStarts.get(0);
      for (Map.Entry<String, Object> param : params.entrySet()) {
        caller = caller.withParam(param.getKey(), param.getValue());
      }
      return ((SubProcessCallStartParamCaller) caller).call().asMap();
    });
  }
}
