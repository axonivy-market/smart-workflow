package com.axonivy.utils.smart.workflow.demo.shopping.product;

import ch.ivyteam.ivy.scripting.objects.List;
import dev.langchain4j.model.output.structured.Description;

public class ProductList {
  @Description("A list of filtered products")
  private List<Product> products;

  public List<Product> getProducts() {
    return products;
  }

  public void setProducts(List<Product> products) {
    this.products = products;
  }

}
