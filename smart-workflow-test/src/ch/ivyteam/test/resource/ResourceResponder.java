package ch.ivyteam.test.resource;

import java.io.IOException;
import java.io.InputStream;

import javax.ws.rs.core.Response;

public class ResourceResponder {

  private final Class<?> testClass;

  public ResourceResponder(Class<?> testClass) {
    this.testClass = testClass;
  }

  public Response send(String resource) {
    return Response.ok()
        .entity(load(resource))
        .build();
  }

  public String load(String resource) {
    try (InputStream is = testClass.getResourceAsStream(resource)) {
      return new String(is.readAllBytes());
    } catch (IOException ex) {
      throw new RuntimeException("Failed to load " + resource, ex);
    }
  }

}
