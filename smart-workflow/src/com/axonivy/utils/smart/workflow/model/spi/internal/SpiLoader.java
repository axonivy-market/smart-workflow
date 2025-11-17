package com.axonivy.utils.smart.workflow.model.spi.internal;

import static ch.ivyteam.ivy.application.ProcessModelVersionRelation.DEPENDENT;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import ch.ivyteam.ivy.application.IProcessModelVersion;
import ch.ivyteam.ivy.application.ReleaseState;
import ch.ivyteam.ivy.java.IJavaConfiguration;
import ch.ivyteam.ivy.project.model.Project;

public class SpiLoader {

  private final Project project;

  public SpiLoader(Project project) {
    this.project = project;
  }

  public <T> Set<T> load(Class<T> type) {
    return projectsInScope()
        .flatMap(p -> findImpl(p, type).stream())
        .distinct()
        .collect(Collectors.toSet());
  }

  private Stream<Project> projectsInScope() {
    var pmv = IProcessModelVersion.of(project);
    var dependendees = pmv.getAllRelatedProcessModelVersions(DEPENDENT)
        .stream()
        .filter(candidate -> ReleaseState.RELEASED.equals(candidate.getReleaseState()))
        .map(IProcessModelVersion::project);
    return Stream.concat(Stream.of(project), dependendees);
  }

  private static <T> List<T> findImpl(Project project, Class<T> type) {
    IJavaConfiguration java = IJavaConfiguration.of(project);
    ClassLoader loader = java.getClassLoader();
    var refs = loadRefs(type, loader);
    var implNames = refs.stream()
        .flatMap(ref -> ref.lines().findFirst().stream())
        .toList();
    return implNames.stream().flatMap(ref -> {
      Optional<T> impl = load(loader, ref);
      return impl.stream();
    })
        .toList();
  }

  private static <T> Optional<T> load(ClassLoader loader, String typeName) {
    try {
      var loaded = loader.loadClass(typeName);
      @SuppressWarnings("unchecked")
      var instance = (T) loaded.getConstructor().newInstance();
      return Optional.of(instance);
    } catch (Exception ex) {
      return Optional.empty();
    }
  }

  private static Set<String> loadRefs(Class<?> type, ClassLoader loader) {
    Set<String> implRefs = new HashSet<>();
    try {
      Enumeration<URL> enumeration = loader.getResources("META-INF/services/" + type.getName());
      var it = enumeration.asIterator();
      while (it.hasNext()) {
        var uri = it.next();
        var in = uri.openStream();
        var what = read(in);
        implRefs.add(what);
      }
    } catch (Exception ex) {

    }
    return implRefs;
  }

  private static String read(InputStream service) {
    try (service) {
      return new String(service.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException ex) {
      throw new RuntimeException("Failed to read service descriptor " + service, ex);
    }
  }
}
