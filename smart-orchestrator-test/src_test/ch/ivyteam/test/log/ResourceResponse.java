package ch.ivyteam.test.log;

import java.io.IOException;
import java.io.InputStream;

import javax.ws.rs.core.Response;

import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class ResourceResponse implements BeforeEachCallback {

  private Class<?> testClass;

  @Override
  public void beforeEach(ExtensionContext context) throws Exception {
    this.testClass = context.getRequiredTestClass();
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
