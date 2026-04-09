package com.axonivy.utils.smart.workflow.demo.tool;

import java.util.List;
import java.util.Map;

import com.axonivy.utils.ai.Invoice;
import com.axonivy.utils.ai.InvoiceItem;
import com.axonivy.utils.smart.workflow.tools.provider.SmartWorkflowTool;

public class TaxCalculatorTool implements SmartWorkflowTool {

  private static final String TAX_LINE_FORMAT = "%s: $%.2f @ %d%% tax = $%.2f%n";
  private static final String[]  ELECTRONICS_KEYWORDS = {"laptop","macbook","computer","phone","television","tv","monitor","tablet","electronics","samsung","apple","dell","lenovo"};
  private static final String[] FOOD_KEYWORDS = {"coffee","food","drink","beverage","meal","snack","grocery"};
  private static final String[] LUXURY_KEYWORDS = {"rolex","luxury","diamond","gold","jewel","watch","premium"};
  private static final String[] SERVICE_KEYWORDS = {"service","consulting","consultation","legal","accounting","support"};

  public record TaxCalculationResult(double totalTax, double effectiveTaxRate, double totalWithTax, String breakdown) {}

  @Override
  public String name() {
    return "calculateTax";
  }

  @Override
  public String description() {
    return """
        Calculate tax for invoice items based on their descriptions and amounts.
        Classifies each item into a tax category and applies the appropriate rate:
        electronics 10%, food/beverage 5%, luxury goods 20%, services 8%, other 10%.
        Returns the total tax, effective tax rate, total amount with tax, and a per-item breakdown.""";
  }

  @Override
  public List<ToolParameter> parameters() {
    return List.of(
        new ToolParameter("invoice",
            "The structured invoice containing line items to calculate tax for",
            "com.axonivy.utils.ai.Invoice"));
  }

  @Override
  public Object execute(Map<String, Object> args) {
    Invoice invoice = (Invoice) args.get("invoice");

    double subtotal = 0;
    double totalTax = 0;
    StringBuilder lineItemTaxBuilder = new StringBuilder();

    for (InvoiceItem item : invoice.getItems()) {
      String description = item.getDescription();
      double amount = item.getTotal();
      double taxRate = getTaxRate(description);
      double taxAmount = amount * taxRate;

      subtotal += amount;
      totalTax += taxAmount;

      lineItemTaxBuilder.append(formatTaxLine(description, amount, taxRate, taxAmount));
    }

    return new TaxCalculationResult(
        totalTax,
        subtotal > 0 ? totalTax / subtotal : 0,
        subtotal + totalTax,
        lineItemTaxBuilder.toString().trim());
  }

  private static double getTaxRate(String description) {
    String lower = description.toLowerCase();
    return switch (lower) {
      case String text when containsAny(text, ELECTRONICS_KEYWORDS) -> 0.10;
      case String text when containsAny(text, FOOD_KEYWORDS) -> 0.05;
      case String text when containsAny(text, LUXURY_KEYWORDS)  -> 0.20;
      case String text when containsAny(text, SERVICE_KEYWORDS) -> 0.08;
      default -> 0.10;
    };
  }

  private static String formatTaxLine(String description, double amount, double taxRate, double taxAmount) {
    int taxRatePercent = (int) (taxRate * 100);
    return String.format(TAX_LINE_FORMAT, description, amount, taxRatePercent, taxAmount);
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
