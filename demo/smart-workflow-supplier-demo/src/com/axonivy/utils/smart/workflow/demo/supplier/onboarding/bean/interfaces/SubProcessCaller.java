package com.axonivy.utils.smart.workflow.demo.supplier.onboarding.bean.interfaces;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import ch.ivyteam.ivy.process.call.SubProcessCallStartEvent;
import ch.ivyteam.ivy.process.call.SubProcessSearchFilter;
import ch.ivyteam.ivy.process.call.SubProcessSearchFilter.SearchScope;
import ch.ivyteam.ivy.security.exec.Sudo;

public interface SubProcessCaller {

  static Map<String, Object> callSubProcess(String signature, Map<String, Object> params) {
    return Sudo.get(() -> {
      var filter = SubProcessSearchFilter.create()
          .setSearchScope(SearchScope.APPLICATION)
          .setSignature(signature)
          .toFilter();

      var candidates = SubProcessCallStartEvent.find(filter);
      if (candidates.isEmpty()) {
        return Map.of();
      }

      var start = candidates.getFirst();

      if (params == null || params.isEmpty()) {
        return start.call().asMap();
      }

      List<Map.Entry<String, Object>> entries = new ArrayList<>(params.entrySet());
      for (int i = 0; i < entries.size() - 1; i++) {
        start.withParam(entries.get(i).getKey(), entries.get(i).getValue());
      }
      var last = entries.getLast();
      return start.withParam(last.getKey(), last.getValue()).call().asMap();
    });
  }
}
