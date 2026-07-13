package com.axonivy.utils.smart.workflow.spi.internal;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import ch.ivyteam.ivy.application.IProcessModelVersion;

public class SpiLoader {
  private final IProcessModelVersion pmv;

  private static final String SERVICES_LOCATION_PATTERN = "META-INF/services/%s";
  private static final String EXCEPTION_PATTERN = "Failed to read service descriptor %s";

  public SpiLoader(IProcessModelVersion pmv) {
    this.pmv = pmv;
  }

  public <T> Set<T> load(Class<T> type) {
    Map<String, T> seen = new HashMap<>();
    List<T> implementations = pmvsInScope()
      .flatMap(p -> findImpl(p, type).stream())
      .filter(type::isInstance)
      .toList();
    for (T impl : implementations) {
      seen.putIfAbsent(impl.getClass().getName(), impl);
    }
    return new HashSet<>(seen.values());
  }

  protected Stream<IProcessModelVersion> pmvsInScope() {
    return Stream.concat(Stream.of(pmv), pmv.getAllDependentProcessModelVersions());
  }

  private static <T> List<T> findImpl(IProcessModelVersion pmv, Class<T> type) {
    ClassLoader loader = ProjectClassLoader.of(pmv);
    return findImpl(type, loader);
  }

  public static <T> List<T> findImpl(Class<T> type, ClassLoader loader) {
    var refs = loadRefs(type, loader);
    var implNames = refs.stream()
        .flatMap(ref -> ref.lines().findFirst().stream())
        .toList();
    return implNames.stream().flatMap(ref -> {
      Optional<T> impl = load(loader, ref);
      return impl.stream();
    }).toList();
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
      throw new RuntimeException(String.format(EXCEPTION_PATTERN, service, ex));
    }
  }
}
