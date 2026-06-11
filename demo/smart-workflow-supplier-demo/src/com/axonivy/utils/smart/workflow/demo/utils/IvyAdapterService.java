package com.axonivy.utils.smart.workflow.demo.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Predicate;

import org.apache.commons.collections4.CollectionUtils;

import ch.ivyteam.ivy.process.call.SubProcessCallStartEvent;
import ch.ivyteam.ivy.process.call.SubProcessSearchFilter;
import ch.ivyteam.ivy.process.call.SubProcessSearchFilter.SearchScope;
import ch.ivyteam.ivy.security.exec.Sudo;

public class IvyAdapterService {

  public static Map<String, Object> startSubProcessInApplication(String signature, Map<String, Object> params) {
    return startSubProcess(signature, params, SearchScope.APPLICATION);
  }

  public static Map<String, Object> startSubProcessInSecurityContext(String signature, Map<String, Object> params) {
    return startSubProcess(signature, params, SearchScope.SECURITY_CONTEXT);
  }

  public static List<Map<String, Object>> startSubProcessesInSecurityContext(String signature, Map<String, Object> params) {
    return Sudo.get(() -> {
      var result = new ArrayList<Map<String, Object>>();

      var filter = SubProcessSearchFilter.create()
          .setSearchScope(SearchScope.SECURITY_CONTEXT)
          .setSignature(signature).toFilter();

      var subProcessStartList = SubProcessCallStartEvent.find(filter);
      if (CollectionUtils.isEmpty(subProcessStartList)) {
        return null;
      }

      subProcessStartList.forEach(subProcessStart -> {
        result.add(Optional.ofNullable(params).map(Map::entrySet).isEmpty() ?
            subProcessStart.call().asMap() :
              startSubProcessWithParams(subProcessStart, params));
      });

      return result;
    });
  }

  public static Map<String, Object> startSubProcessesInSecurityContext(String signature, Map<String, Object> params, Predicate<Map<String, Object>> collectCondition) {
    return Sudo.get(() -> {
      var filter = SubProcessSearchFilter.create()
          .setSearchScope(SearchScope.SECURITY_CONTEXT)
          .setSignature(signature).toFilter();

      var subProcessStartList = SubProcessCallStartEvent.find(filter);
      if (CollectionUtils.isEmpty(subProcessStartList)) {
        return new HashMap<>();
      }

      for (SubProcessCallStartEvent subProcess : subProcessStartList) {
        Map<String, Object> result =  Optional.ofNullable(params).map(Map::entrySet).isEmpty() ?
            subProcess.call().asMap() : startSubProcessWithParams(subProcess, params);
        if (collectCondition.test(result)) {
          return result;
        }
      }

      return new HashMap<>();
    });
  }

  public static Map<String, Object> startSubProcessInProjectAndAllRequired(String signature, Map<String, Object> params) {
    return startSubProcess(signature, params, SearchScope.PROJECT_AND_ALL_REQUIRED);
  }

  private static Map<String, Object> startSubProcess(String signature, Map<String, Object> params, SearchScope scope) {
    return Sudo.get(() -> {
      var filter = SubProcessSearchFilter.create()
          .setSearchScope(scope)
          .setSignature(signature).toFilter();

      var subProcessStartList = SubProcessCallStartEvent.find(filter);
      if (CollectionUtils.isEmpty(subProcessStartList)) {
        return null;
      }
      var subProcessStart = subProcessStartList.get(0);

      return Optional.ofNullable(params).map(Map::entrySet).isEmpty() ?
        subProcessStart.call().asMap() :
          startSubProcessWithParams(subProcessStart, params);
    });
  }

  private static Map<String, Object> startSubProcessWithParams(SubProcessCallStartEvent subProcess, Map<String, Object> params) {
    Map<String, Object> result = null;
    List<Entry<String, Object>> entryList = new ArrayList<>(params.entrySet());

    for(Entry<String, Object> entry : entryList) {
      if (entryList.indexOf(entry) != entryList.size() - 1) {
        subProcess.withParam(entry.getKey(), entry.getValue());
      } else {
        result = subProcess.withParam(entry.getKey(), entry.getValue()).call().asMap();
      }
    }

    return result;
  }
}
