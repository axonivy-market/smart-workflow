package com.axonivy.utils.smart.workflow.program.internal;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.smart.workflow.extraction.internal.CmsLoader;
import com.axonivy.utils.smart.workflow.extraction.internal.FileExtractor;

import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.process.program.exec.ProgramContext;
import ch.ivyteam.ivy.scripting.objects.Binary;
import ch.ivyteam.ivy.workflow.document.IDocument;
import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;

public class QueryExpander {

  private static final Pattern SCRIPT_VARIABLE_PATTERN = Pattern.compile("<%=(.+?)%>");
  private static final Pattern CMS_EXPRESSION_PATTERN = Pattern.compile("^ivy\\.cms\\.co\\([\"'](.+?)[\"']\\)$");

  private record InputStreamWithName(InputStream stream, String name) implements AutoCloseable {
    InputStreamWithName(Path path) throws IOException {
      this(Files.newInputStream(path), path.getFileName().toString());
    }
    InputStreamWithName(byte[] bytes) {
      this(new ByteArrayInputStream(bytes), null);
    }

    @Override
    public void close() throws IOException {
      stream.close();
    }
  }

  public static Optional<String> expandMacro(String confKey, ProgramContext context) {
    try {
      var template = Optional.ofNullable(context.config().get(confKey));
      if (template.isEmpty()) {
        return Optional.empty();
      }

      var expanded = context.script().expandMacro(template.get());
      return Optional.ofNullable(expanded).filter(Predicate.not(String::isBlank));
    } catch (Exception ex) {
      Ivy.log().error(ex.getMessage(), ex);
      return Optional.empty();
    }
  }

  public static Optional<UserMessage> expandMacroWithFileExtraction(String confKey, ProgramContext context, ChatModel model) {
    try {
        var template = Optional.ofNullable(context.config().get(confKey));
        if (template.isEmpty()) {
          return Optional.empty();
        }

        UserMessage userMessage = expandFileExpressions(
          template.get(),
          model,
          expr -> context.script().executeExpression(expr, Object.class),
          CmsLoader::load);

        return Optional.of(userMessage);
    } catch (Exception ex) {
      Ivy.log().error(ex.getMessage(), ex);
      return Optional.empty();
    }
  }

  static UserMessage expandFileExpressions(String template, ChatModel model, Function<String, Optional<Object>> resolver, Function<String, Object> cmsLoader) {
    List<Content> contents = new ArrayList<>();
    Matcher matcher = SCRIPT_VARIABLE_PATTERN.matcher(template);
    int lastEnd = 0;
    while (matcher.find()) {
      String textBefore = template.substring(lastEnd, matcher.start());
      if (!textBefore.isEmpty()) {
        contents.add(TextContent.from(textBefore));
      }

      String expression = matcher.group(1).trim();
      String cmsPath = extractCmsPath(expression);
      Optional<Content> value = cmsPath != null
          ? extractFromCms(cmsPath, model, cmsLoader)
          : extractFromExpression(expression, resolver, model);

      if (value.isPresent()) {
        contents.add(value.get());
      } else {
        contents.add(TextContent.from(matcher.group(0)));
      }

      lastEnd = matcher.end();
    }

    String tail = template.substring(lastEnd);
    if (!tail.isEmpty()) {
      contents.add(TextContent.from(tail));
    }

    return UserMessage.from(contents);
  }

  private static Optional<Content> extractFromCms(String cmsPath, ChatModel model, Function<String, Object> cmsLoader) {
    Object cmsValue = cmsLoader.apply(cmsPath);
    return switch (cmsValue) {
      case String str -> Optional.of(TextContent.from(str));
      case InputStream stream -> new FileExtractor(model).extract(stream, null);
      case null, default -> Optional.of(TextContent.from(""));
    };
  }

  private static Optional<Content> extractFromExpression(String expression, Function<String, Optional<Object>> resolver, ChatModel model) {
    if (StringUtils.isBlank(expression)) {
      return Optional.empty();
    }
    Object target = resolver.apply(expression).orElse(null);
    InputStreamWithName source = toInputStreamWithName(target);
    if (source != null) {
      try (source) {
        return new FileExtractor(model).extract(source.stream(), source.name());
      } catch (IOException e) {
        throw new RuntimeException("Failed to close stream for expression '" + expression + "'", e);
      }
    }
    return Optional.ofNullable(target).map(String::valueOf).map(TextContent::from);
  }

  private static InputStreamWithName toInputStreamWithName(Object value) {
    try {
      return switch (value) {
        case null -> null;
        case InputStream stream
          -> new InputStreamWithName(stream, null);
        case Path path
          -> new InputStreamWithName(path);
        case File javaFile
          -> new InputStreamWithName(javaFile.toPath());
        case ch.ivyteam.ivy.scripting.objects.File ivyFile
          -> new InputStreamWithName(ivyFile.getJavaFile().toPath());
        case IDocument document
          -> new InputStreamWithName(document.read().asStream(), document.getName());
        case Binary binary
          -> new InputStreamWithName(binary.toByteArray());
        default -> null;
      };
    } catch (IOException ex) {
      throw new RuntimeException("Failed to open stream for value: " + value, ex);
    }
  }

  private static String extractCmsPath(String expression) {
    Matcher matcher = CMS_EXPRESSION_PATTERN.matcher(expression.trim());
    return matcher.matches() ? matcher.group(1) : null;
  }
}
