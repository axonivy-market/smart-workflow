package com.axonivy.utils.smart.orchestrator.demo.shopping.store;

import java.io.Serializable;

import com.axonivy.utils.smart.orchestrator.demo.shopping.product.Product;

public class CartItem implements Serializable {

  private static final long serialVersionUID = -7818505222120227465L;

  private Product product;
  private int quantity;

  public CartItem() {
    this.quantity = 1;
  }

  public CartItem(Product product) {
    this.product = product;
    this.quantity = 1;
  }

  public CartItem(Product product, int quantity) {
    this.product = product;
    this.quantity = quantity;
  }

  public Product getProduct() {
    return product;
  }

  public void setProduct(Product product) {
    this.product = product;
  }

  public int getQuantity() {
    return quantity;
  }

  public void setQuantity(int quantity) {
    this.quantity = quantity;
  }

  public double getTotalPrice() {
    if (product != null && product.getUnitPrice() != null) {
      try {
        return Double.parseDouble(product.getUnitPrice()) * quantity;
      } catch (NumberFormatException e) {
        return 0.0;
      }
    }
    return 0.0;
  }

  public String getFormattedTotalPrice() {
    return String.format("%.2f", getTotalPrice());
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null || getClass() != obj.getClass())
      return false;
    CartItem cartItem = (CartItem) obj;
    return product != null && product.getProductId() != null
        && product.getProductId().equals(cartItem.product.getProductId());
  }

  @Override
  public int hashCode() {
    return product != null && product.getProductId() != null ? product.getProductId().hashCode() : 0;
  }
}