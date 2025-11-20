package com.axonivy.utils.smart.workflow.model.spi.internal;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import ch.ivyteam.ivy.application.IProcessModelVersion;

public class SpiLoader {

  private final IProcessModelVersion pmv;

  public SpiLoader(IProcessModelVersion myPmv) {
    this.pmv = myPmv;
  }

  public <T> Set<T> load(Class<T> type) {
    return findImpl(SpiLoader.class.getClassLoader(), type);
  }

  private static <T> Set<T> findImpl(ClassLoader loader, Class<T> type) {
    var refs = loadRefs(type, loader);
    var implNames = refs.stream()
        .flatMap(ref -> ref.lines().findFirst().stream())
        .toList();
    return implNames.stream().flatMap(ref -> {
      Optional<T> impl = load(loader, ref);
      return impl.stream();
    })
        .collect(Collectors.toSet());
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
