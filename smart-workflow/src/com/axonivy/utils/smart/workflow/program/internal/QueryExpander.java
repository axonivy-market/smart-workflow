package com.axonivy.utils.smart.workflow.program.internal;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
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

        Matcher matcher = SCRIPT_VARIABLE_PATTERN.matcher(template.get());

        // Step 1: Replace all <%=expr%> with UUID placeholders
        Map<String, String> placeholders = new LinkedHashMap<>();
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String expression = matcher.group(1).trim();
            String uuid = UUID.randomUUID().toString();
            placeholders.put(uuid, expression);
            matcher.appendReplacement(result, uuid);
        }
        matcher.appendTail(result);

        // Step 2: Resolve each placeholder
        String expanded = result.toString();
        boolean hasOtherExpressions = false;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            File file = getFile(entry.getValue(), context);
            if (file != null) {
                // File expression: extract content using AI and replace
                String content = new FileExtractor(model).extract(file);
                expanded = expanded.replace(entry.getKey(), content);
            } else {
                // Non-file expression: restore original <%=expr%>
                expanded = expanded.replace(entry.getKey(), "<%=" + entry.getValue() + "%>");
                hasOtherExpressions = true;
            }
        }

        // If there are still unreplaced <%=expr%> patterns, delegate to macro engine for final expansion
        if (hasOtherExpressions) {
            expanded = context.script().expandMacro(expanded);
        }
        return Optional.ofNullable(expanded).filter(Predicate.not(String::isBlank));
    } catch (Exception ex) {
      return Optional.empty();
    }
  }

  private static File getFile(String expression, ProgramContext context) {
    if (StringUtils.isBlank(expression)) {
      return null;
    }
    try {
      Object returnValue = context.script().executeExpression(expression, Object.class).orElse(null);
      return switch (returnValue) {
        case null -> null;
        case File javaFile -> javaFile;
        case ch.ivyteam.ivy.scripting.objects.File ivyFile -> ivyFile.getJavaFile();
        default -> null;
      };
    } catch (RuntimeException ex) {
      throw new RuntimeException("Failed to read '" + expression + "'", ex);
    }
  }
}
