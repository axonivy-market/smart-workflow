package com.axonivy.utils.smart.workflow.spi.internal;

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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.MethodUtils;

import ch.ivyteam.ivy.application.IProcessModelVersion;
import static ch.ivyteam.ivy.application.ProcessModelVersionRelation.DEPENDENT;
import ch.ivyteam.ivy.project.model.Project;

public class SpiLoader {
  private final Project project;

  private static final String IJAVA_CONFIGURATION = "ch.ivyteam.ivy.java.IJavaConfiguration";
  private static final String SERVICES_LOCATION_PATTERN = "META-INF/services/%s";
  private static final String EXCEPTION_PATTERN = "Failed to read service descriptor %s";

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
        .map(IProcessModelVersion::project);
    return Stream.concat(Stream.of(project), dependendees);
  }

  private static <T> List<T> findImpl(Project project, Class<T> type) {
    ClassLoader loader = loaderOf(project);
    Set<String> refs = loadRefs(type, loader);
    return refs.stream().flatMap(className -> {
      Optional<T> impl = load(loader, className);
      return impl.stream();
    }).toList();
  }

  private static ClassLoader loaderOf(Project project) {
    try {
      var javaConf = Class.forName(IJAVA_CONFIGURATION);
      var of = MethodUtils.getMethodObject(javaConf, "of", Project.class);
      var local = of.invoke(null, project);
      var loader = MethodUtils.getMethodObject(javaConf, "getClassLoader");
      return (ClassLoader) loader.invoke(local);
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
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
      Enumeration<URL> enumeration = loader.getResources(String.format(SERVICES_LOCATION_PATTERN, type.getName()));
      var it = enumeration.asIterator();
      while (it.hasNext()) {
        var uri = it.next();
        var in = uri.openStream();
        Set<String> what = read(in);
        implRefs.addAll(what);
      }
    } catch (Exception ex) {
      return Set.of();
    }
    return implRefs;
  }

  private static Set<String> read(InputStream service) {
    try (service) {
      String services = new String(service.readAllBytes(), StandardCharsets.UTF_8);
      return services.lines()
          .filter(line -> StringUtils.isNotBlank(line))
          .collect(Collectors.toSet());
    } catch (IOException ex) {
      throw new RuntimeException(String.format(EXCEPTION_PATTERN, service, ex));
    }
  }
}
