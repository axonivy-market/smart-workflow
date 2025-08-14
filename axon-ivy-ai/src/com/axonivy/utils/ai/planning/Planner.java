package com.axonivy.utils.ai.planning;

import com.axonivy.utils.ai.connector.OpenAiServiceConnector;

import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public class Planner {

  private ChatModel model;
  private ChatMemoryProvider memoryProvider;
  private String memoryId;

  public Planner(ChatModel model, ChatMemoryProvider memoryProvider, String memoryId) {
    this.model = model;
    this.memoryProvider = memoryProvider;
    this.memoryId = memoryId;
  }

  public Planner(ChatMemoryProvider memoryProvider, String memoryId) {
    this.memoryProvider = memoryProvider;
    this.memoryId = memoryId;
  }

  private void buildDefaultModel() {
    this.model = OpenAiServiceConnector.buildJsonOpenAiModel().build();
  }

  public String createPlan(String goal, String query, String toolInfos, String instructions) {
    if (model == null) {
      buildDefaultModel();
    }

    ToolExecutionPlanner plannerInstance = AiServices.builder(ToolExecutionPlanner.class).chatModel(model)
        .chatMemoryProvider(memoryProvider).build();
    String result = plannerInstance.createPlan(goal, query, toolInfos, instructions, memoryId);

    return result;
  }

  public interface ToolExecutionPlanner {
    @SystemMessage("""
        You are a Planner agent coordinating a multi-agent system. Your task is to create a step-by-step plan to fulfill the given goal using only the provided tools.

        You will first analyze the user input, then describe a detailed plan in natural language using only the available tools. Your plan must contain no more than 10 steps. The plan should explain the use of each tool clearly in natural language, following the goal and based only on the given input.

        Respond in the following format:

        ---

        **User message:** <user message>
        **Goal:** {{goal}}
        **Available Tools:**
        {{tools}}

        ---
        **Additional instructions:**
        {{instructions}}

        ---

        **Analysis:**
        - Describe what the user is trying to achieve in your own words.
        - Extract relevant details or entities from the input.
        - Point out ambiguities or assumptions you’ll need to make.
        - Identify which tools are useful and how they can be sequenced to achieve the goal.

        ---

        **Plan:**
        - Use natural language to describe each step in sequence.
        - Mention the tools by name and their parameters where appropriate.
        - Explain how each step leads to the next.
        - Do not include steps that use tools not listed.
        - If data is missing, either infer it or note it as "unknown".

        Format:
        - First, I will use ToolName to...
        - Then, I will...
        - Then,...
        - ...
        - Finally, I will...

        ---

        Only use the tools provided. Do not generate or assume tools outside the list.
        """)
    @UserMessage("{{query}}")
    public String createPlan(@V("goal") String goal, @V("query") String query, @V("tools") String toolInfos,
        @V("instructions") String instructions, @MemoryId String memoryId);
  }
}
