package com.axonivy.utils.ai.mock;

import java.util.function.Function;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.annotation.security.PermitAll;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import tools.jackson.databind.JsonNode;

@Path(MockAura.PATH_SUFFIX)
@PermitAll
@Hidden
public class MockAura {

  public static final String PATH_SUFFIX = "auraMock";
  private static Function<JsonNode, Response> INFERENCE = _ -> Response.serverError().build();

  public static void defineInference(Function<JsonNode, Response> inference) {
    INFERENCE = inference;
  }

  @POST
  @Path("api/v1/{orgId}/inference")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response inference(JsonNode request, @PathParam("orgId") String orgId) {
    return INFERENCE.apply(request);
  }
}
