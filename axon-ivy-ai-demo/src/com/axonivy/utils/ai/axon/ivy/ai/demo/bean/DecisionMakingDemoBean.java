package com.axonivy.utils.ai.axon.ivy.ai.demo.bean;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.ai.connector.OpenAiServiceConnector;
import com.axonivy.utils.ai.dto.ai.AiExample;
import com.axonivy.utils.ai.dto.ai.AiOption;
import com.axonivy.utils.ai.service.IvyAdapterService;

import ch.ivyteam.ivy.scripting.objects.List;

@ManagedBean
@ViewScoped
public class DecisionMakingDemoBean implements Serializable {

  private static final long serialVersionUID = -4950260131463771254L;

  private List<AiOption> options;
  private List<AiExample> examples;

  private String input;
  private String output;

  public void addNewOption() {
    if (options == null) {
      options = new List<>();
    }
    options.add(new AiOption(StringUtils.EMPTY, StringUtils.EMPTY));
  }

  public void addNewExample() {
    if (examples == null) {
      examples = new List<>();
    }

    examples.add(new AiExample(StringUtils.EMPTY, StringUtils.EMPTY));
  }

  public void runElement() {
    Map<String, Object> params = new HashMap<>();
    params.put("options", options);
    params.put("examples", examples);
    params.put("query", input);
    params.put("customInstructions", null);
    params.put("connector", OpenAiServiceConnector.getTinyBrain());

    String signature = "AiDecide(List<com.axonivy.utils.ai.dto.ai.AiOption>,String,List<com.axonivy.utils.ai.dto.ai.AiExample>,List<String>,com.axonivy.utils.ai.connector.AbstractAiServiceConnector)";

    Map<String, Object> result = IvyAdapterService.startSubProcessInApplication(signature, params);
    output = (String) result.get("result");
  }

  public List<AiOption> getOptions() {
    return options;
  }

  public void setOptions(List<AiOption> options) {
    this.options = options;
  }

  public List<AiExample> getExamples() {
    return examples;
  }

  public void setExamples(List<AiExample> examples) {
    this.examples = examples;
  }

  public String getInput() {
    return input;
  }

  public void setInput(String input) {
    this.input = input;
  }

  public String getOutput() {
    return output;
  }

  public void setOutput(String output) {
    this.output = output;
  }
}
