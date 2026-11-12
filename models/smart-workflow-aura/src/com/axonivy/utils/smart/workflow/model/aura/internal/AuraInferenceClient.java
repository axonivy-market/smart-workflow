package com.axonivy.utils.smart.workflow.model.aura.internal;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.axonivy.utils.smart.workflow.utils.JsonUtils;

import dev.langchain4j.exception.HttpException;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/** Synchronous inference and bounded polling on the configured Aura service. */
public final class AuraInferenceClient {

  private final WebTarget api;
  private final long timeoutNanos;

  /** The supplied target owns authentication; this client never changes request origins. */
  public AuraInferenceClient(WebTarget baseTarget, UUID organization, Duration timeout) {
    if (timeout.isZero() || timeout.isNegative()) {
      throw new IllegalArgumentException("Aura request timeout must be positive");
    }
    this.timeoutNanos = timeout.toNanos();
    this.api = baseTarget.register(AuraHttpClientFeature.class)
        .path("api/v1").path(organization.toString())
        .property("jersey.config.client.followRedirects", false);
  }

  public JsonNode infer(ObjectNode body) {
    long started = System.nanoTime();
    var initial = exchange(api.path("inference"), body, started);
    if (initial.status == 200) {
      return initial.body;
    }
    UUID jobId = jobId(initial.body);
    var job = initial;
    while (true) {
      if (!jobId.equals(jobId(job.body))) {
        throw new IllegalStateException("Aura returned a different inference job ID");
      }
      switch (job.body.path("status").asString("")) {
        case "completed":
          var result = job.body.get("result");
          if (result == null || !result.isObject()) {
            throw new IllegalStateException("Aura completed an inference job without a result");
          }
          return result;
        case "failed":
          throw jobFailure(job.body);
        case "queued", "in_progress":
          waitForPoll(job.retryAfter, started);
          job = exchange(api.path("inference-jobs").path(jobId.toString()), null, started);
          break;
        default:
          throw new IllegalStateException("Aura returned an unknown inference job status");
      }
    }
  }

  private Reply exchange(WebTarget target, ObjectNode body, long started) {
    long remaining = remainingNanos(started);
    int timeoutMillis = (int) Math.min(Integer.MAX_VALUE, Math.max(1, TimeUnit.NANOSECONDS.toMillis(remaining)));
    var request = target
        .property("jersey.config.client.connectTimeout", timeoutMillis)
        .property("jersey.config.client.readTimeout", timeoutMillis)
        .request(MediaType.APPLICATION_JSON_TYPE);
    try (Response response = body == null ? request.get() : request.post(Entity.json(body.toString()))) {
      remainingNanos(started);
      int status = response.getStatus();
      if (status != 200 && !(status == 202 && body != null)) {
        throw httpFailure(response);
      }
      var result = parse(response);
      remainingNanos(started);
      return new Reply(status, result, retryAfter(response.getHeaderString("Retry-After")));
    } catch (ProcessingException ignored) {
      // Jersey exceptions may contain URLs, headers or response bodies. Do not expose their cause.
      throw new IllegalStateException("Aura HTTP request failed; check connectivity and request timeout");
    }
  }

  private static JsonNode parse(Response response) {
    try {
      var body = JsonUtils.getObjectMapper().readTree(response.readEntity(String.class));
      if (body != null && body.isObject()) {
        return body;
      }
    } catch (RuntimeException ignored) {
      // Never include an untrusted response or JSON parser exception in diagnostics.
    }
    throw new IllegalStateException("Aura returned an invalid JSON response");
  }

  private static UUID jobId(JsonNode body) {
    try {
      return UUID.fromString(body.path("job_id").asString(""));
    } catch (IllegalArgumentException ignored) {
      throw new IllegalStateException("Aura returned an invalid inference job ID");
    }
  }

  private long remainingNanos(long started) {
    if (Thread.currentThread().isInterrupted()) {
      throw new IllegalStateException("Aura inference was interrupted");
    }
    long remaining = timeoutNanos - (System.nanoTime() - started);
    if (remaining <= 0) {
      throw new IllegalStateException("Aura inference exceeded its request timeout");
    }
    return remaining;
  }

  private void waitForPoll(Duration retryAfter, long started) {
    long remaining = remainingNanos(started);
    long wait = Math.min(remaining, retryAfter.toNanos());
    try {
      TimeUnit.NANOSECONDS.sleep(wait);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Aura inference was interrupted");
    }
    remainingNanos(started);
  }

  private static Duration retryAfter(String header) {
    try {
      long seconds = Long.parseLong(header);
      if (seconds >= 0 && seconds <= Integer.MAX_VALUE) {
        return Duration.ofMillis(Math.max(100, seconds * 1000));
      }
    } catch (NumberFormatException ignored) {
      // Aura specifies delta seconds, not an HTTP date. Missing/invalid values use a safe default.
    }
    return Duration.ofSeconds(1);
  }

  private static HttpException httpFailure(Response response) {
    int status = response.getStatus();
    String detail = switch (status) {
      case 400, 422 -> "request validation failed; check messages, schema and options";
      case 401 -> "authentication failed; check the bearer token";
      case 403 -> "access denied; check organization permissions and Aura availability";
      case 404 -> "inference job not found, expired or inaccessible";
      case 429 -> rateLimitDetail(response);
      case 503 -> "service unavailable; try again later";
      default -> "unexpected response status; redirects are not followed";
    };
    return new HttpException(status, "Aura HTTP " + status + ": " + detail);
  }

  private static String rateLimitDetail(Response response) {
    try {
      return switch (parse(response).path("error").asString("")) {
        case "TOKEN_LIMIT_EXCEEDED" -> "Aura credit budget exhausted; check the organization's budget";
        case "RATE_LIMIT_EXCEEDED" -> "request rate limit exceeded; try again later";
        default -> "request rate or credit limit exceeded; check Aura limits";
      };
    } catch (IllegalStateException ignored) {
      return "request rate or credit limit exceeded; check Aura limits";
    }
  }

  private static IllegalStateException jobFailure(JsonNode body) {
    String detail = switch (body.path("error").path("code").asString("")) {
      case "content_filter" -> "content filter blocked the request or answer";
      case "token_limit" -> "token budget exhausted";
      case "queue_timeout" -> "timed out waiting for capacity";
      case "interrupted" -> "server interrupted the inference";
      default -> "service could not complete the inference";
    };
    return new IllegalStateException("Aura inference job failed: " + detail);
  }

  private record Reply(int status, JsonNode body, Duration retryAfter) {
  }
}
