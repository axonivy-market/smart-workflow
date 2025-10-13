package com.axonivy.utils.smart.workflow.demo.shopping.store;

import java.util.List;

import com.axonivy.utils.smart.workflow.demo.shopping.product.Product;

import dev.langchain4j.model.output.structured.Description;

public class SuggestionItem {
  @Description("Header description for the selected item. For example, if the chosen item is a hat then the header should be 'hat'")
  private String itemHeader;

  @Description("The style theme or purpose that guides what kind of fashion items should suggest.")
  private String idea;
  
  @Description("Relevant keywords that describle this item")
  private List<String> keywords;

  @Description("The selected item")
  private Product item;

  public String getItemHeader() {
    return itemHeader;
  }

  public void setItemHeader(String itemHeader) {
    this.itemHeader = itemHeader;
  }

  public Product getItem() {
    return item;
  }

  public void setItem(Product item) {
    this.item = item;
  }

  public String getIdea() {
    return idea;
  }

  public void setIdea(String idea) {
    this.idea = idea;
  }

  public List<String> getKeywords() {
    return keywords;
  }

  public void setKeywords(List<String> keywords) {
    this.keywords = keywords;
  }
}
