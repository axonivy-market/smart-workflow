package com.axonivy.utils.smart.workflow.guardrails;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.axonivy.utils.smart.workflow.guardrails.adapter.GuardrailInspectionText;

import dev.langchain4j.data.message.AudioContent;
import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.PdfFileContent;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.message.VideoContent;

class TestGuardrailInspectionText {

  @Test
  void returnsEmptyForNullMessage() {
    assertThat(GuardrailInspectionText.text(null)).isEmpty();
  }

  @Test
  void returnsTextAsIsForTextOnlyMessage() {
    var message = UserMessage.from(TextContent.from("What is the weather?"));

    assertThat(GuardrailInspectionText.text(message)).isEqualTo("What is the weather?");
  }

  @Test
  void joinsMultipleTextPartsWithNewline() {
    var message = UserMessage.from(TextContent.from("first"), TextContent.from("second"));

    assertThat(GuardrailInspectionText.text(message)).isEqualTo("first\nsecond");
  }

  @Test
  void doesNotThrowOnMultimodalMessage_regressionForSingleTextBug() {
    var message = UserMessage.from(
        TextContent.from("describe this:"),
        ImageContent.from("http://example.com/img.png", "image/png"));

    assertThat(GuardrailInspectionText.text(message)).isEqualTo("describe this:\n<file: IMAGE>");
  }

  @ParameterizedTest
  @MethodSource("filePlaceholders")
  void replacesFileContentWithPlaceholder(FilePlaceholderCase testCase) {
    var message = UserMessage.from(testCase.content());

    assertThat(GuardrailInspectionText.text(message)).isEqualTo("<file: " + testCase.expectedType() + ">");
  }

  record FilePlaceholderCase(Content content, String expectedType) {}

  static Stream<FilePlaceholderCase> filePlaceholders() {
    return Stream.of(
        new FilePlaceholderCase(ImageContent.from("http://example.com/img.png", "image/png"), "IMAGE"),
        new FilePlaceholderCase(PdfFileContent.from("http://example.com/doc.pdf", "application/pdf"), "PDF"),
        new FilePlaceholderCase(AudioContent.from("http://example.com/clip.mp3", "audio/mp3"), "AUDIO"),
        new FilePlaceholderCase(VideoContent.from("http://example.com/clip.mp4", "video/mp4"), "VIDEO"));
  }
}
