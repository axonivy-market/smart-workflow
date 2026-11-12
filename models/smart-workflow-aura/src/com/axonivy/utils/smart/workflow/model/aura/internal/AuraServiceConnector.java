package com.axonivy.utils.smart.workflow.model.aura.internal;

import java.net.URI;
import java.time.Duration;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

import com.axonivy.utils.smart.workflow.model.spi.ModelOptions;

import ch.ivyteam.ivy.environment.Ivy;
import jakarta.ws.rs.client.ClientRequestFilter;

public class AuraServiceConnector {

  public static final String REST_CLIENT = "aura";

  public interface AuraConf {
    String PREFIX = "AI.Providers.Aura.";
    String BASE_URL = PREFIX + "BaseUrl";
    String ORGANIZATION_ID = PREFIX + "OrganizationId";
    String BEARER_TOKEN = PREFIX + "BearerToken";
    String TIMEOUT_SECONDS = PREFIX + "TimeoutSeconds";
  }

  public static AuraChatModel buildChatModel(ModelOptions options) {
    URI baseUrl = baseUrl();
    UUID organization = organization();
    Duration timeout = timeout();
    String token = required(AuraConf.BEARER_TOKEN);
    if (token.chars().anyMatch(c -> c <= 32 || c >= 127)) {
      throw new IllegalArgumentException("Aura BearerToken must contain only the token, without spaces or line breaks");
    }
    // Work on a child target, so per-call credentials never accumulate on a shared client.
    var service = Ivy.rest().client(REST_CLIENT).path("");
    // A REST-client override must retain the approved service root, not just its host.
    if (!Strings.CS.equals(StringUtils.stripEnd(baseUrl.getRawPath(), "/"),
        StringUtils.stripEnd(service.getUri().getRawPath(), "/"))) {
      throw new IllegalStateException("Aura refused to send credentials to a different service");
    }
    var target = service.register((ClientRequestFilter) request -> {
      URI destination = request.getUri();
      if (!baseUrl.getScheme().equalsIgnoreCase(destination.getScheme())
          || !baseUrl.getRawAuthority().equalsIgnoreCase(destination.getRawAuthority())) {
        throw new IllegalStateException("Aura refused to send credentials to a different service");
      }
      request.getHeaders().putSingle("Authorization", "Bearer " + token);
    });
    return new AuraChatModel(new AuraInferenceClient(target, organization, timeout), options);
  }

  private static URI baseUrl() {
    String value = required(AuraConf.BASE_URL);
    URI uri;
    try {
      uri = URI.create(value);
    } catch (IllegalArgumentException ignored) {
      throw new IllegalArgumentException("Aura BaseUrl must be an absolute service URL");
    }
    boolean https = "https".equalsIgnoreCase(uri.getScheme());
    boolean localHttp = "http".equalsIgnoreCase(uri.getScheme())
        && ("localhost".equalsIgnoreCase(uri.getHost()) || "127.0.0.1".equals(uri.getHost())
            || "[::1]".equals(uri.getHost()));
    if ((!https && !localHttp) || uri.getHost() == null || uri.getRawUserInfo() != null
        || uri.getRawQuery() != null || uri.getRawFragment() != null) {
      throw new IllegalArgumentException("Aura BaseUrl requires HTTPS (HTTP is allowed only on loopback), without credentials, query or fragment");
    }
    return uri;
  }

  private static UUID organization() {
    try {
      return UUID.fromString(required(AuraConf.ORGANIZATION_ID));
    } catch (IllegalArgumentException ignored) {
      throw new IllegalArgumentException("Aura OrganizationId must be a UUID");
    }
  }

  private static Duration timeout() {
    String value = StringUtils.defaultIfBlank(Ivy.var().get(AuraConf.TIMEOUT_SECONDS), "600");
    try {
      int seconds = Integer.parseInt(value);
      if (seconds > 0) {
        return Duration.ofSeconds(seconds);
      }
    } catch (NumberFormatException ignored) {
      // Do not include configuration values in errors.
    }
    throw new IllegalArgumentException("Aura TimeoutSeconds must be a positive integer");
  }

  private static String required(String variable) {
    String value = Ivy.var().get(variable);
    if (StringUtils.isBlank(value)) {
      throw new IllegalArgumentException("Missing Aura configuration: " + variable);
    }
    return value.strip();
  }
}
