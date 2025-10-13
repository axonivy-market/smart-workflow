package com.axonivy.utils.smart.workflow.demo.shopping.store;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import dev.langchain4j.model.output.structured.Description;

public class Suggestion {

  @Description("Original user message")
  private String message;

  @Description("Original budget use as the base to make suggestion")
  private Float budget;

  @Description("Items for the suggestion")
  private List<SuggestionItem> items;

  @Description("The style theme or purpose that guides what kind of fashion items should suggest.")
  private List<String> ideas;

  @JsonIgnore
  private Float total;

  public Float getBudget() {
    return budget;
  }

  public void setBudget(Float budget) {
    this.budget = budget;
  }

  public List<SuggestionItem> getItems() {
    return items;
  }

  public void setItems(List<SuggestionItem> items) {
    this.items = items;
  }

  public Float getTotal() {
    return total;
  }

  public void setTotal(Float total) {
    this.total = total;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public List<String> getIdeas() {
    return ideas;
  }

  public void setIdeas(List<String> ideas) {
    this.ideas = ideas;
  }
}