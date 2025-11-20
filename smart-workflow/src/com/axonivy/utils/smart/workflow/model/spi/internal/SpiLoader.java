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

public class SpiLoader {

  private final IProcessModelVersion pmv;

  public SpiLoader(IProcessModelVersion myPmv) {
    this.pmv = myPmv;
  }

  public <T> Set<T> load(Class<T> type) {
    return projectsInScope()
        .flatMap(p -> findImpl(p, type).stream())
        .distinct()
        .collect(Collectors.toSet());
  }

  private Stream<IProcessModelVersion> projectsInScope() {
    var dependendees = pmv.getAllRelatedProcessModelVersions(DEPENDENT)
        .stream()
        .filter(candidate -> ReleaseState.RELEASED.equals(candidate.getReleaseState()));
    return Stream.concat(Stream.of(pmv), dependendees);
  }

  private static <T> List<T> findImpl(IProcessModelVersion pmv, Class<T> type) {
    var javaInternal = ch.ivyteam.ivy.java.IJavaConfiguration.of(pmv.project());
    return findImpl(type, javaInternal.getClassLoader());
  }

  private static <T> List<T> findImpl(Class<T> type, ClassLoader loader) {
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
        var what = read(uri.openStream());
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
