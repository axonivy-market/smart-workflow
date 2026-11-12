package com.axonivy.utils.ai.mock;

import java.util.function.Function;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.annotation.security.PermitAll;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import tools.jackson.databind.JsonNode;

@Path("aiMock/aura")
@PermitAll
@Hidden
@Produces(MediaType.APPLICATION_JSON)
public class MockAura {

  private static volatile Function<Request, Response> handler = MockAura::unconfigured;

  public static void define(Function<Request, Response> responseHandler) {
    handler = responseHandler;
  }

  public static void reset() {
    handler = MockAura::unconfigured;
  }

  private static Response unconfigured(Request request) {
    return Response.serverError().entity("{}").build();
  }

  @POST
  @Path("api/v1/{org}/inference")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response infer(JsonNode body, @PathParam("org") String organization,
      @HeaderParam("Authorization") String authorization) {
    return handler.apply(new Request("POST", organization, null, body, authorization));
  }

  @GET
  @Path("api/v1/{org}/inference-jobs/{job}")
  public Response poll(@PathParam("org") String organization, @PathParam("job") String job,
      @HeaderParam("Authorization") String authorization) {
    return handler.apply(new Request("GET", organization, job, null, authorization));
  }

  @GET
  @Path("redirect-target")
  public Response redirected() {
    return handler.apply(new Request("REDIRECT", null, null, null, null));
  }

  public record Request(String method, String organization, String jobId, JsonNode body, String authorization) {
  }
}
