package com.axonivy.utils.smart.workflow.demo.erp.procurement.model;

import java.io.Serializable;
import java.util.UUID;

import dev.langchain4j.model.output.structured.Description;
import com.axonivy.utils.smart.workflow.demo.erp.procurement.agent.feedback.FeedbackOption;

public class ProcurementItem implements Serializable {

  private static final long serialVersionUID = 1L;

  @Description("Unique identifier for the material item within the procurement request")
  private String id;
  @Description("Reference to the material type being procured")
  private String materialTypeId;
  @Description("Position of the item in the procurement request list, used for ordering")
  private Integer position;
  @Description("Detailed description of the material, including specifications or requirements")
  private String materialDescription;
  @Description("Standard grade or class of the material, if applicable")
  private String standardGradeClass;
  @Description("Quantity of the material being requested")
  private Double quantity;
  @Description("Unit of measurement for the quantity (e.g., pieces, kg, meters)")
  private String unit;
  @Description("Preferred supplier for the material, if any")
  private String supplier;
  @Description("Net price per unit of the material")
  private Double unitPriceNet;
  @Description("Total net price for the requested quantity (calculated as quantity * unitPriceNet)")
  private Double totalPriceNet;
  @Description("Purpose or intended use of the material within the project")
  private String purpose;
  @Description("Additional comments or notes about the material item")
  private String comment;
  @Description("Indicates whether this material item was flagged as problematic during analysis (e.g., insufficient stock or not found in inventory)")
  private boolean hasTrouble;

  @Description("If the agent identified an issue with this material item, this field contains the feedback option suggesting how to resolve it (e.g., alternative materials or suppliers). This is only populated if hasTrouble is true.")
  private FeedbackOption feedbackOption;

  @Description("Indicates whether this item was recently changed by an automated replacement (e.g., applyFeedbackReplacements). Reset to false on each new agent message.")
  private boolean changed;

  private String aiComment;

  public ProcurementItem() {
    this.id = UUID.randomUUID().toString();
    this.quantity = 0.0;
    this.unitPriceNet = 0.0;
    this.totalPriceNet = 0.0;
  }

  public void calculateTotal() {
    if (quantity == null || unitPriceNet == null) {
      this.totalPriceNet = 0.0;
    } else {
      this.totalPriceNet = quantity * unitPriceNet;
    }
  }

  // Getters and Setters

  public String getId() {
    if (id == null) { id = UUID.randomUUID().toString(); }
    return id;
  }
  public void setId(String id) { this.id = id; }

  public String getMaterialTypeId() { return materialTypeId; }
  public void setMaterialTypeId(String materialTypeId) { this.materialTypeId = materialTypeId; }

  public Integer getPosition() { return position; }
  public void setPosition(Integer position) { this.position = position; }

  public String getMaterialDescription() { return materialDescription; }
  public void setMaterialDescription(String materialDescription) { this.materialDescription = materialDescription; }

  public String getStandardGradeClass() { return standardGradeClass; }
  public void setStandardGradeClass(String standardGradeClass) { this.standardGradeClass = standardGradeClass; }

  public Double getQuantity() { return quantity; }
  public void setQuantity(Double quantity) { this.quantity = quantity; }

  public String getUnit() { return unit; }
  public void setUnit(String unit) { this.unit = unit; }

  public String getSupplier() { return supplier; }
  public void setSupplier(String supplier) { this.supplier = supplier; }

  public Double getUnitPriceNet() { return unitPriceNet; }
  public void setUnitPriceNet(Double unitPriceNet) { this.unitPriceNet = unitPriceNet; }

  public Double getTotalPriceNet() { return totalPriceNet; }
  public void setTotalPriceNet(Double totalPriceNet) { this.totalPriceNet = totalPriceNet; }

  public String getPurpose() { return purpose; }
  public void setPurpose(String purpose) { this.purpose = purpose; }

  public String getComment() { return comment; }
  public void setComment(String comment) { this.comment = comment; }

  public boolean isHasTrouble() { return hasTrouble; }
  public void setHasTrouble(boolean hasTrouble) { this.hasTrouble = hasTrouble; }

  public FeedbackOption getFeedbackOption() { return feedbackOption; }
  public void setFeedbackOption(FeedbackOption feedbackOption) { this.feedbackOption = feedbackOption; }

  public boolean isChanged() { return changed; }
  public void setChanged(boolean changed) { this.changed = changed; }

  public String getAiComment() { return aiComment; }
  public void setAiComment(String aiComment) { this.aiComment = aiComment; }
}
