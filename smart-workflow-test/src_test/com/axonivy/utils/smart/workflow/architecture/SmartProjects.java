package com.axonivy.utils.smart.workflow.architecture;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

interface SmartProjects {

  static interface Dirs {
    Path REPO_ROOT = Paths.get("..").normalize();
    Path MODELS_DIR = REPO_ROOT.resolve("models");
    Path SMART_WORKFLOW_DIR = REPO_ROOT.resolve("smart-workflow");
    Path SMART_WORKFLOW_TEST_DIR = REPO_ROOT.resolve("smart-workflow-test");
  }

  public static Path core() {
    return Dirs.SMART_WORKFLOW_DIR.resolve("target/classes");
  }

  public static List<Path> models() throws IOException {
    return projectClassDirs(Dirs.MODELS_DIR, Integer.MAX_VALUE);
  }

  public static List<Path> consumersNoModels() throws IOException {
    var consumers = new ArrayList<>(consumers());
    var models = models();
    consumers.removeAll(models);
    return consumers;
  }

  public static List<Path> consumers() throws IOException {
    return projectClassDirs(Dirs.REPO_ROOT, 3).stream()
        .filter(classes -> !Dirs.SMART_WORKFLOW_DIR.equals(projectOf(classes)))
        .filter(classes -> !Dirs.SMART_WORKFLOW_TEST_DIR.equals(projectOf(classes)))
        .toList();
  }

  private static Path projectOf(Path classDir) {
    return classDir.getParent().getParent();
  }

  private static List<Path> projectClassDirs(Path root, int maxDepth) throws IOException {
    try (var projects = Files.walk(root, maxDepth)) {
      return projects
          .filter(path -> path.getFileName().toString().equals("pom.xml"))
          .map(Path::getParent)
          .filter(SmartProjects::hasJavaSources)
          .map(projectDir -> projectDir.resolve("target/classes"))
          .filter(Files::isDirectory)
          .toList();
    }
  }

  private static boolean hasJavaSources(Path projectDir) {
    return Files.isDirectory(projectDir.resolve("src")) ||
        Files.isDirectory(projectDir.resolve("src_generated"));
  }
}
