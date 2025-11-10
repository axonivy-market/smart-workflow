package com.axonivy.utils.smart.workflow.decision;

import java.util.ArrayList;
import java.util.List;

import com.axonivy.utils.smart.workflow.model.openai.internal.OpenAiServiceConnector;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public class DecisionMaker {

  private ChatModel model;
  private List<Option> options;
  private List<String> instructions;

  public static Builder getBuilder() {
    return new Builder();
  }

  private interface IDecisionMaker {

    @SystemMessage("""
        OPTIONS (ID - Condition pairs):
        {{options}}
        ---
        INSTRUCTIONS:
        1. Analyze the user message carefully.
        2. Select the most suitable option based on the provided conditions.
        3. Respond ONLY with a JSON in the following format:
           {
             "id": "<selected_option_id>",
             "condition": "<selected_option_condition>"
           }
        4. Do NOT include any additional explanation or text.
        ---
        ADDITIONAL INSTRUCTIONS:
        {{instructions}}
          """)
    @UserMessage("{{message}}")
    public Option makeDecision(@V("message") String message, @V("options") List<Option> options,
        @V("instructions") List<String> instructions);
  }

  public Option makeDecision(String message) {
    IDecisionMaker decisionMaker = AiServices.builder(IDecisionMaker.class).chatModel(model).build();
    return decisionMaker.makeDecision(message, options, instructions);
  }

  private void setModel(ChatModel model) {
    this.model = model;
  }

  private void setOptions(List<Option> options) {
    this.options = options;
  }

  private void setInstructions(List<String> instructions) {
    this.instructions = instructions;
  }

  public static class Builder {
    private ChatModel model = OpenAiServiceConnector.buildJsonOpenAiModel().build();;
    private List<Option> options = new ArrayList<>();
    private List<String> instructions = new ArrayList<>();

    public Builder model(ChatModel model) {
      this.model = model;
      return this;
    }

    public Builder options(List<Option> options) {
      if (options != null) {
        this.options = options;
      }
      return this;
    }

    public Builder instructions(List<String> instructions) {
      if (instructions != null) {
        this.instructions = instructions;
      }
      return this;
    }

    public DecisionMaker build() {
      DecisionMaker decisionMaker = new DecisionMaker();
      decisionMaker.setModel(model);
      decisionMaker.setOptions(options);
      decisionMaker.setInstructions(instructions);
      return decisionMaker;
    }
  }
}
