package com.axonivy.utils.smart.workflow.guardrails.internal;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

import com.axonivy.utils.smart.workflow.guardrails.internal.annotation.SmartWorkflowGuardrail;
import com.axonivy.utils.smart.workflow.guardrails.internal.annotation.SmartWorkflowGuardrail.GuardrailType;

import ch.ivyteam.ivy.application.IProcessModelVersion;
import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.java.IJavaConfiguration;
import ch.ivyteam.ivy.project.model.Project;
import dev.langchain4j.guardrail.InputGuardrail;
import dev.langchain4j.guardrail.OutputGuardrail;

/**
 * Finder utility to discover all classes annotated with
 * {@link SmartWorkflowGuardrail} in the current project and its dependent
 * projects.
 */
public class GuardrailFinder extends ClassVisitor {

  private static final String ANNOTATION_DESC = "Lcom/axonivy/utils/smart/workflow/guardrails/internal/annotation/SmartWorkflowGuardrail;";

  private boolean annotated = false;
  private String className;
  private GuardrailType guardrailType = GuardrailType.INPUT;

  private static record LoadContext(Project project, String className) {}

  public GuardrailFinder() {
    super(Opcodes.ASM9);
  }

  @Override
  public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
    this.className = name.replace('/', '.');
  }

  @Override
  public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
    if (ANNOTATION_DESC.equals(descriptor)) {
      annotated = true;
      return new AnnotationVisitor(Opcodes.ASM9) {
        @Override
        public void visitEnum(String name, String descriptor, String value) {
          if ("type".equals(name)) {
            guardrailType = GuardrailType.valueOf(value);
          }
        }
      };
    }
    return super.visitAnnotation(descriptor, visible);
  }

  public boolean isAnnotated() {
    return annotated;
  }

  public String getClassName() {
    return className;
  }

  public GuardrailType getGuardrailType() {
    return guardrailType;
  }

  public static List<String> findInputGuardrailClassNames() {
    return findGuardrailClassNames(GuardrailType.INPUT);
  }

  public static List<String> findOutputGuardrailClassNames() {
    return findGuardrailClassNames(GuardrailType.OUTPUT);
  }

  public static List<String> findGuardrailClassNames(GuardrailType type) {
    try {
      return projectsInScope().stream().flatMap(p -> findInProject(p, type).stream()).distinct()
          .collect(Collectors.toList());
    } catch (Exception ex) {
      String typeName = type == GuardrailType.INPUT ? "input" : "output";
      Ivy.log().error(String.format("Failed to find %s guardrail classes", typeName), ex);
      return new ArrayList<>();
    }
  }

  public static List<InputGuardrail> findInputGuardrailsByClassNames(List<String> classNames) {
    return findGuardrailsByClassNames(classNames, GuardrailFinder::loadInputGuardrail);
  }

  public static List<OutputGuardrail> findOutputGuardrailsByClassNames(List<String> classNames) {
    return findGuardrailsByClassNames(classNames, GuardrailFinder::loadOutputGuardrail);
  }

  private static <T> List<T> findGuardrailsByClassNames(List<String> classNames, Function<LoadContext, T> loader) {
    if (CollectionUtils.isEmpty(classNames)) {
      return new ArrayList<>();
    }

    List<Project> projectsInScope = projectsInScope();
    return classNames.stream()
        .map(
            className -> projectsInScope.stream().map(
                project -> loader.apply(new LoadContext(project, className)))
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(null))
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  private static InputGuardrail loadInputGuardrail(LoadContext ctx) {
    return loadGuardrail(ctx, GuardrailType.INPUT, InputGuardrail.class);
  }

  private static OutputGuardrail loadOutputGuardrail(LoadContext ctx) {
    return loadGuardrail(ctx, GuardrailType.OUTPUT, OutputGuardrail.class);
  }

  private static <T> T loadGuardrail(LoadContext ctx, GuardrailType expectedType, Class<T> expectedInterface) {
    if (StringUtils.isEmpty(ctx.className)) {
      return null;
    }
    try {
      Class<?> clazz = IJavaConfiguration.of(ctx.project).getClassLoader().loadClass(ctx.className);
      SmartWorkflowGuardrail annotation = clazz.getAnnotation(SmartWorkflowGuardrail.class);
      if (annotation == null || annotation.type() != expectedType || !expectedInterface.isAssignableFrom(clazz)) {
        return null;
      }
      return expectedInterface.cast(clazz.getDeclaredConstructor().newInstance());
    } catch (Exception ex) {
      return null;
    }
  }

  private static List<Project> projectsInScope() {
    var currentProject = IProcessModelVersion.current().project();
    var pmv = IProcessModelVersion.of(currentProject);
    List<Project> result = new ArrayList<>();
    result.add(currentProject);

    result.addAll(pmv.getLibrary().getAllRequiredLibraries().stream()
        .map(requiredPmv -> requiredPmv.getProcessModelVersion().project()).toList());

    result.addAll(pmv.getLibrary().getAllDependentLibraries().stream()
        .map(dependedPmv -> dependedPmv.getProcessModelVersion().project()).toList());

    return result;
  }

  private static List<String> findInProject(Project project, GuardrailType type) {
    try {
      IJavaConfiguration javaConfig = IJavaConfiguration.of(project);
      List<String> guardrailClasses = new ArrayList<>();

      // Scan output directories where compiled .class files are located
      var outputFolders = javaConfig.getOutputFolders();
      for (var outputFolder : outputFolders) {
        if (outputFolder.exists()) {
          guardrailClasses.addAll(scanDirectory(outputFolder.getLocation().toPath(), type));
        }
      }

      return guardrailClasses;
    } catch (Exception ex) {
      Ivy.log().warn("Failed to scan project: " + project.name(), ex);
      return new ArrayList<>();
    }
  }

  private static List<String> scanDirectory(Path directory, GuardrailType type) {
    List<String> classes = new ArrayList<>();
    try {
      if (!Files.exists(directory)) {
        return classes;
      }

      try (Stream<Path> paths = Files.walk(directory)) {
        paths.filter(path -> path.toString().endsWith(".class")).forEach(classFile -> {
          try (InputStream stream = Files.newInputStream(classFile)) {
            ClassReader reader = new ClassReader(stream);
            GuardrailFinder finder = new GuardrailFinder();
            reader.accept(finder, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);

            if (finder.isAnnotated() && finder.getClassName() != null && finder.getGuardrailType() == type) {
              classes.add(finder.getClassName());
            }
          } catch (IOException ex) {
            Ivy.log().debug("Failed to read class file: " + classFile, ex);
          }
        });
      }
    } catch (Exception ex) {
      Ivy.log().debug("Failed to scan directory: " + directory, ex);
    }
    return classes;
  }
}
