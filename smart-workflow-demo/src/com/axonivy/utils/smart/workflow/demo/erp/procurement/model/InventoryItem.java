package com.axonivy.utils.smart.workflow.demo.erp.procurement.model;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

/**
 * Represents the current inventory stock level for a specific material type.
 * Each entry tracks the available quantity, unit, and last restocked date.
 */
public class InventoryItem implements Serializable {

  private static final long serialVersionUID = 1L;

  private String id;
  private String materialTypeId;
  private String materialTypeName;
  private String materialItemId;
  private Double quantityAvailable;
  private Double quantityReserved;
  private String unit;
  private String location;
  private String notes;
  private Date lastRestockedDate;
  private Date lastUpdatedDate;

  /** Populated at runtime by the repository — not persisted. */
  private transient ProcurementItem materialItem;

  public InventoryItem() {
    this.id = UUID.randomUUID().toString();
    this.quantityAvailable = 0.0;
    this.quantityReserved = 0.0;
    this.lastUpdatedDate = new Date();
  }

  public InventoryItem(String materialTypeId, String materialTypeName, String unit) {
    this();
    this.materialTypeId = materialTypeId;
    this.materialTypeName = materialTypeName;
    this.unit = unit;
  }

  public InventoryItem(String materialTypeId, String materialTypeName, String materialItemId, String unit) {
    this(materialTypeId, materialTypeName, unit);
    this.materialItemId = materialItemId;
  }

  /**
   * Returns the quantity that is free to use (available minus reserved).
   */
  public Double getQuantityFree() {
    double available = quantityAvailable != null ? quantityAvailable : 0.0;
    double reserved = quantityReserved != null ? quantityReserved : 0.0;
    return Math.max(0.0, available - reserved);
  }

  public String getId() { return id; }
  public void setId(String id) { this.id = id; }

  public String getMaterialTypeId() { return materialTypeId; }
  public void setMaterialTypeId(String materialTypeId) { this.materialTypeId = materialTypeId; }

  public String getMaterialTypeName() { return materialTypeName; }
  public void setMaterialTypeName(String materialTypeName) { this.materialTypeName = materialTypeName; }

  public String getMaterialItemId() { return materialItemId; }
  public void setMaterialItemId(String materialItemId) { this.materialItemId = materialItemId; }

  public ProcurementItem getMaterialItem() { return materialItem; }
  public void setMaterialItem(ProcurementItem materialItem) { this.materialItem = materialItem; }

  public Double getQuantityAvailable() { return quantityAvailable; }
  public void setQuantityAvailable(Double quantityAvailable) {
    this.quantityAvailable = quantityAvailable;
    this.lastUpdatedDate = new Date();
  }

  public Double getQuantityReserved() { return quantityReserved; }
  public void setQuantityReserved(Double quantityReserved) { this.quantityReserved = quantityReserved; }

  public String getUnit() { return unit; }
  public void setUnit(String unit) { this.unit = unit; }

  public String getLocation() { return location; }
  public void setLocation(String location) { this.location = location; }

  public String getNotes() { return notes; }
  public void setNotes(String notes) { this.notes = notes; }

  public Date getLastRestockedDate() { return lastRestockedDate; }
  public void setLastRestockedDate(Date lastRestockedDate) { this.lastRestockedDate = lastRestockedDate; }

  public Date getLastUpdatedDate() { return lastUpdatedDate; }
  public void setLastUpdatedDate(Date lastUpdatedDate) { this.lastUpdatedDate = lastUpdatedDate; }
}
