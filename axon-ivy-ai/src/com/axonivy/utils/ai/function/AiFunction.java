package com.axonivy.utils.ai.function;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.ai.connector.AbstractAiServiceConnector;
import com.axonivy.utils.ai.dto.ai.AiExample;
import com.axonivy.utils.ai.dto.ai.AiVariable;
import com.axonivy.utils.ai.enums.AiVariableState;
import com.axonivy.utils.ai.utils.StringProcessingUtils;

import dev.langchain4j.model.input.PromptTemplate;

public abstract class AiFunction {
  protected static final String RESULT_PREFIX = "<<";
  protected static final String RESULT_POSTFIX = ">>";

  // Format an example to format:
  // Input: example input
  // Output: example output
  //
  private static final String EXAMPLE_LINE_PATTERN = """
          Query: %s
          Expected result: %s

      """;

  // The general prompt template
  protected static final String PROMPT_TEMPLATE = """
      Query:
      {{query}}
      -------------------------------
      Instruction:
      - {{functionInstructions}}
      {{customInstructions}}
      {{wrapperInstruction}}

      {{examples}}
      """;

  private static final String WRAPPER_INSTRUCTION = """
      - Put the result inside '<<' and '>>'.
          + Example of a plain text output: <<This is the result>>
          + Example of a json output: <<{"name":"test"}>>
          + Example of a multi-line output:
          <<This is a line
           another line

           another line>>
      """;

  // Template for the examples part
  private static final String EXAMPLES_TEMPLATE = """
      -------------------------------
      Below are some examples of correct result:

      {{examples}}
      """;

  // The input query
  private String query;

  // Instructions to guide AI how to execute the function
  private String functionInstructions;

  // Instructions to guide AI how to show the result
  private List<String> outputInstructions;

  // The external instructions that modify or refine the function's behavior
  private List<String> customInstructions;

  // Set of examples of how the result look like
  private List<AiExample> examples;

  // Abstract method to build instructions before execute
  protected abstract void buildInstructions();

  // AI service connector to handle the communication with AI provider
  private AbstractAiServiceConnector connector;

  // Option to put the result into wrapper characters or not. default is false
  private Boolean useWrappers = false;

  // Option to show the result as plain text or not. Default is true
  private Boolean isPlainText = true;

  protected boolean failedToBuildInstructions;

  private String prompt;

  protected abstract AiVariable createStandardResult(String resultFromAI);

  // Method to execute this function
  public AiVariable execute() {
    // Build instructions
    buildInstructions();

    // If error occurred when building instructions, return empty result
    if (failedToBuildInstructions) {
      failedToBuildInstructions = false;
      return createStandardResult(StringUtils.EMPTY);
    }

    // Map parameters
    Map<String, Object> params = new HashMap<>();
    params.put("query", Optional.ofNullable(query).orElse(StringUtils.EMPTY));
    params.put("functionInstructions", Optional.ofNullable(functionInstructions).orElse(StringUtils.EMPTY));
    params.put("customInstructions", getFormattedCustomInstructions());
    params.put("wrapperInstruction", (isPlainText || !useWrappers) ? StringUtils.EMPTY : WRAPPER_INSTRUCTION);
    params.put("examples", getFormattedExamples());

    // Use AI to perform action
    prompt = PromptTemplate.from(PROMPT_TEMPLATE).apply(params).text().strip();
    String resultFromAI = connector.generate(prompt);

    // standardized the result from AI
    resultFromAI = isPlainText ? resultFromAI : standardizeResult(resultFromAI);

    // return the final result
    return createStandardResult(resultFromAI);
  }

  // Method to standardize result from AI
  // If cannot extract the standardize result, return null instead
  protected String standardizeResult(String result) {
    return StringProcessingUtils.standardizeResult(result, true);
  }

  public String getFunctionInstructions() {
    return this.functionInstructions;
  }

  public void setFunctionInstructions(String functionInstructions) {
    this.functionInstructions = functionInstructions;
  }

  public List<String> getCustomInstructions() {
    return this.customInstructions;
  }

  public void setCustomInstructions(List<String> customInstructions) {
    this.customInstructions = customInstructions;
  }

  public List<String> getOutputInstructions() {
    return this.outputInstructions;
  }

  public void setOutputInstructions(List<String> outputInstructions) {
    this.outputInstructions = outputInstructions;
  }

  public String getQuery() {
    return this.query;
  }

  public void setQuery(String query) {
    this.query = query;
  }

  public AbstractAiServiceConnector getConnector() {
    return connector;
  }

  public void setConnector(AbstractAiServiceConnector connector) {
    this.connector = connector;
  }

  public Boolean getIsPlainText() {
    return isPlainText;
  }

  public void setIsPlainText(Boolean isPlainText) {
    this.isPlainText = isPlainText;
  }

  public Boolean getUseWrappers() {
    return useWrappers;
  }

  public void setUseWrappers(Boolean useWrappers) {
    this.useWrappers = useWrappers;
  }

  private String getFormattedCustomInstructions() {

    // If there is no instructions, just return an empty string
    if (CollectionUtils.isEmpty(customInstructions)) {
      return "";
    }

    // Format each instruction in a single line with the leading '-' character
    String result = StringUtils.EMPTY;
    for (String item : customInstructions) {
      result = result.concat(String.format("- %s\n", item));
    }

    return result;
  }

  private String getFormattedExamples() {
    // If there is no example, return empty string
    if (examples == null || examples.isEmpty()) {
      return "";
    }

    // Format examples using the EXAMPLE_LINE_PATTERN String pattern
    String result = StringUtils.EMPTY;
    for (AiExample item : examples) {
      result = result.concat(String.format(EXAMPLE_LINE_PATTERN, item.getQuery(), item.getExpectedResult()));
    }

    // Map the formatted examples into the examples template
    Map<String, Object> params = new HashMap<>();
    params.put("examples", result);

    return PromptTemplate.from(EXAMPLES_TEMPLATE).apply(params).text();
  }

  public void setExamples(List<AiExample> examples) {
    this.examples = examples;
  }

  protected AiVariable buildErrorResult() {
    AiVariable result = new AiVariable();
    result.init();
    result.getParameter().setValue("Error occurred when AI generating the result");
    result.setState(AiVariableState.ERROR);
    result.getParameter().setDescription("Error occurred when AI generating the result");
    return result;
  }

  public String getPrompt() {
    return prompt;
  }

  public void setPrompt(String prompt) {
    this.prompt = prompt;
  }

  public static class Builder {
    protected AbstractAiServiceConnector connector;
    protected String query = StringUtils.EMPTY;
    protected List<String> customInstructions = new ArrayList<>();
    protected List<AiExample> examples = new ArrayList<>();

    public Builder useService(AbstractAiServiceConnector connector) {
      this.connector = connector;
      return this;
    }

    public Builder withQuery(String query) {
      this.query = query;
      return this;
    }

    public Builder addCustomInstruction(String instruction) {
      if (StringUtils.isNotBlank(instruction)) {
        this.customInstructions.add(instruction);
      }
      return this;
    }

    public Builder addExample(List<AiExample> examples) {
      if (StringUtils.isNotBlank(query)) {
        this.examples.addAll(examples);
      }
      return this;
    }

    public AiFunction build() {
      return new AiFunction() {

        @Override
        protected void buildInstructions() {
        }

        @Override
        protected AiVariable createStandardResult(String resultFromAI) {
          // TODO Auto-generated method stub
          return null;
        }
      };
    }
  }
}