package com.axonivy.utils.smart.workflow.program.internal;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.smart.workflow.extraction.FileExtractor;

import ch.ivyteam.ivy.process.program.exec.ProgramContext;
import dev.langchain4j.model.chat.ChatModel;

public class QueryExpander {

  // Matches Ivy script variables like <%=in.demoFile%>
  private static final Pattern SCRIPT_VARIABLE_PATTERN = Pattern.compile("<%=(.+?)%>");

  public static Optional<String> expandMacro(String confKey, ProgramContext context) {
    try {
      var template = Optional.ofNullable(context.config().get(confKey));
      if (template.isEmpty()) {
        return Optional.empty();
      }

      var expanded = context.script().expandMacro(template.get());
      return Optional.ofNullable(expanded).filter(Predicate.not(String::isBlank));
    } catch (Exception ex) {
      return Optional.empty();
    }
  }

  public static Optional<String> expandMacroWithFileExtraction(String confKey, ProgramContext context, ChatModel model) {
    try {
        var template = Optional.ofNullable(context.config().get(confKey));
        if (template.isEmpty()) {
          return Optional.empty();
        }

        String expanded = expandFileExpressions(template.get(),
            expr -> context.script().executeExpression(expr, Object.class), model);
        // Final pass to expand any remaining script variables after file extraction
        if (SCRIPT_VARIABLE_PATTERN.matcher(expanded).find()) {
            expanded = context.script().expandMacro(expanded);
        }
        return Optional.ofNullable(expanded).filter(Predicate.not(String::isBlank));
    } catch (Exception ex) {
      return Optional.empty();
    }
  }

  /**
   * Iterates each {@code <%=expr%>} in the template and replaces it via {@link #extractFromExpression}.
   */
  static String expandFileExpressions(String template, Function<String, Optional<Object>> resolver, ChatModel model) {
    Matcher matcher = SCRIPT_VARIABLE_PATTERN.matcher(template);
    StringBuilder result = new StringBuilder();
    while (matcher.find()) {
      String expression = matcher.group(1).trim();
      Optional<String> extracted = extractFromExpression(expression, resolver, model);
      if (extracted.isPresent()) {
        matcher.appendReplacement(result, Matcher.quoteReplacement(extracted.get()));
      }
    }
    matcher.appendTail(result);
    return result.toString();
  }

  /** 
   * Returns AI-extracted content if the expression resolves to a file-like resource.
   */
  private static Optional<String> extractFromExpression(String expression, Function<String, Optional<Object>> resolver, ChatModel model) {
    if (StringUtils.isBlank(expression)) {
      return Optional.empty();
    }
    Object target = resolver.apply(expression).orElse(null);
    Path path = toPath(target);
    if (path == null) {
      return Optional.empty();
    }
    return Optional.ofNullable(performExtraction(path, model));
  }

  private static String performExtraction(Path path, ChatModel model) {
    try {
      InputStream stream = Files.newInputStream(path);
      String fileName = path.getFileName().toString();
      return new FileExtractor(model).extract(stream, fileName);
    } catch (IOException ex) {
      throw new RuntimeException("Failed to open stream for path: " + path, ex);
    }
  }

  private static Path toPath(Object value) {
    return switch (value) {
      case null -> null;
      case File javaFile -> javaFile.toPath();
      case Path path -> path;
      case ch.ivyteam.ivy.scripting.objects.File ivyFile -> ivyFile.getJavaFile().toPath();
      default -> null;
    };
  }
}
