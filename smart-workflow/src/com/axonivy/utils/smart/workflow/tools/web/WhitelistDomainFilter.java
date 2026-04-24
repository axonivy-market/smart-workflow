package com.axonivy.utils.smart.workflow.tools.web;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import ch.ivyteam.ivy.environment.Ivy;

public class WhitelistDomainFilter {

  private final Set<String> whitelistDomains;

  public WhitelistDomainFilter() {
    this(readWhitelistDomains());
  }

  WhitelistDomainFilter(Set<String> whitelistDomains) {
    this.whitelistDomains = whitelistDomains;
  }

  public List<SmartWebSearchResult> filter(List<SmartWebSearchResult> results) {
    if (whitelistDomains.isEmpty()) {
      return results;
    }
    return results.stream()
        .filter(result -> isAllowedDomain(result.url()))
        .collect(Collectors.toList());
  }

  boolean isAllowedDomain(String url) {
    try {
      String host = URI.create(url).getHost();
      if (host == null) {
        return false;
      }
      return whitelistDomains.stream()
          .anyMatch(domain -> host.equals(domain) || host.endsWith("." + domain));
    } catch (Exception e) {
      return false;
    }
  }

  static Set<String> readWhitelistDomains() {
    var configuredValue = StringUtils.defaultString(Ivy.var().get(WebSearchConf.WHITELIST_DOMAINS));
    return Arrays.stream(StringUtils.split(configuredValue, ','))
        .map(String::strip)
        .map(String::toLowerCase)
        .filter(StringUtils::isNotBlank)
        .collect(Collectors.toSet());
  }
}
