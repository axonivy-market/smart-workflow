package com.axonivy.utils.smart.workflow.demo.tool;

import java.util.List;
import java.util.Map;

import com.axonivy.utils.smart.workflow.tools.entity.SmartWorkflowTool;
import com.axonivy.utils.smart.workflow.tools.entity.ToolParameter;
import com.axonivy.utils.smart.workflow.tools.entity.ToolParameter.ParameterType;

public class TaxCalculatorTool implements SmartWorkflowTool {

  public record TaxCalculationResult(double totalTax, double effectiveTaxRate, double totalWithTax, String breakdown) {}

  @Override
  public String name() {
    return "calculateTax";
  }

  @Override
  public String description() {
    return "Calculate tax for invoice items based on their descriptions and amounts. "
        + "Classifies each item into a tax category and applies the appropriate rate: "
        + "electronics 10%, food/beverage 5%, luxury goods 20%, services 8%, other 10%. "
        + "Returns the total tax, effective tax rate, total amount with tax, and a per-item breakdown.";
  }

  @Override
  public List<ToolParameter> parameters() {
    return List.of(
        ToolParameter.of("itemDescriptions",
            "List of item descriptions used to classify the tax category for each item",
            ParameterType.STRING_ARRAY),
        ToolParameter.of("itemAmounts",
            "Corresponding list of item amounts in the same order as the descriptions",
            ParameterType.NUMBER_ARRAY));
  }

  @Override
  @SuppressWarnings("unchecked")
  public Object execute(Map<String, Object> args) {
    List<String> itemDescriptions = (List<String>) args.get("itemDescriptions");
    List<Double> itemAmounts = (List<Double>) args.get("itemAmounts");

    double subtotal = 0;
    double totalTax = 0;
    var breakdownBuilder = new StringBuilder();

    int size = Math.min(itemDescriptions.size(), itemAmounts.size());
    for (int i = 0; i < size; i++) {
      String description = itemDescriptions.get(i);
      double amount = itemAmounts.get(i);
      double taxRate = getTaxRate(description);
      double taxAmount = amount * taxRate;

      subtotal += amount;
      totalTax += taxAmount;

      breakdownBuilder
          .append(description)
          .append(": $").append(String.format("%.2f", amount))
          .append(" @ ").append((int) (taxRate * 100)).append("% tax")
          .append(" = $").append(String.format("%.2f", taxAmount))
          .append("\n");
    }

    return new TaxCalculationResult(
        totalTax,
        subtotal > 0 ? totalTax / subtotal : 0,
        subtotal + totalTax,
        breakdownBuilder.toString().trim());
  }

  private static double getTaxRate(String description) {
    String lower = description.toLowerCase();
    if (containsAny(lower, "laptop", "macbook", "computer", "phone", "television", "tv", "monitor",
        "tablet", "electronics", "samsung", "apple", "dell", "lenovo")) {
      return 0.10;
    }
    if (containsAny(lower, "coffee", "food", "drink", "beverage", "meal", "snack", "grocery")) {
      return 0.05;
    }
    if (containsAny(lower, "rolex", "luxury", "diamond", "gold", "jewel", "watch", "premium")) {
      return 0.20;
    }
    if (containsAny(lower, "service", "consulting", "consultation", "legal", "accounting", "support")) {
      return 0.08;
    }
    return 0.10;
  }

  private static boolean containsAny(String text, String... keywords) {
    for (String keyword : keywords) {
      if (text.contains(keyword)) {
        return true;
      }
    }
    return false;
  }
}
