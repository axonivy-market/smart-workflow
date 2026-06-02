package com.axonivy.utils.smart.workflow.market;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.axonivy.utils.smart.workflow.utils.JsonUtils;
import com.fasterxml.jackson.databind.JsonNode;

public class ProductJson {

  private static final JsonNode PRODUCT = productJson();

  public static List<String> installerArtifactIds() {
    return installerArtifactIds(PRODUCT);
  }

  private static JsonNode productJson() {
    try {
      var where = ProductJson.class.getResource("/").toURI();
      var repo = Path.of(where).getParent().getParent().getParent();
      var product = repo.resolve("smart-workflow-product").resolve("product.json");
      try(var in = Files.newInputStream(product, StandardOpenOption.READ)){
        return JsonUtils.getObjectMapper().readTree(in);
      }
    } catch (Exception e) {
      throw new RuntimeException("Failed to load product.json for test", e);
    }
  }

  private static List<String> installerArtifactIds(JsonNode product) {
    return stream(product.path("installers"))
        .flatMap(installer -> stream(installer.path("data").path("projects")))
        .map(project -> project.path("artifactId").asText())
        .toList();
  }

  private static Stream<JsonNode> stream(JsonNode node) {
    return StreamSupport.stream(node.spliterator(), false);
  }
}
