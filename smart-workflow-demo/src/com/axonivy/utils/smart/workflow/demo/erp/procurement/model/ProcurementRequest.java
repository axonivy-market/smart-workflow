package com.axonivy.utils.smart.workflow.demo.erp.procurement.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ProcurementRequest implements Serializable {

  private static final long serialVersionUID = 1L;

  private String id;
  private String projectName;
  private String projectNumberCostCenter;
  private String constructionSiteDeliveryAddress;
  private String requiredDeliveryDate;
  private RequestPriority priority;
  private String requester;
  private String orderNotes;
  private String requestDate;
  private List<ProcurementItem> materialItems;
  private RequestStatus status;
  private Double totalNetAmount;
  private String createdDate;
  private String lastModifiedDate;
  private String caseUuid;

  public ProcurementRequest() {
    this.id = UUID.randomUUID().toString();
    this.priority = RequestPriority.NORMAL;
    this.status = RequestStatus.DRAFT;
    this.requestDate = LocalDate.now().toString();
    this.createdDate = LocalDate.now().toString();
    this.lastModifiedDate = LocalDate.now().toString();
    this.materialItems = new ArrayList<>();
    this.totalNetAmount = 0.0;
  }

  public void calculateTotalNetAmount() {
    double total = 0.0;
    for (ProcurementItem item : materialItems) {
      item.calculateTotal();
      if (item.getTotalPriceNet() != null) {
        total += item.getTotalPriceNet();
      }
    }
    this.totalNetAmount = total;
  }

  public void reindexItems() {
    for (int i = 0; i < materialItems.size(); i++) {
      materialItems.get(i).setPosition(i + 1);
    }
  }

  // Getters and Setters

  public String getId() { return id; }
  public void setId(String id) { this.id = id; }

  public String getProjectName() { return projectName; }
  public void setProjectName(String projectName) { this.projectName = projectName; }

  public String getProjectNumberCostCenter() { return projectNumberCostCenter; }
  public void setProjectNumberCostCenter(String projectNumberCostCenter) { this.projectNumberCostCenter = projectNumberCostCenter; }

  public String getConstructionSiteDeliveryAddress() { return constructionSiteDeliveryAddress; }
  public void setConstructionSiteDeliveryAddress(String constructionSiteDeliveryAddress) { this.constructionSiteDeliveryAddress = constructionSiteDeliveryAddress; }

  public String getRequiredDeliveryDate() { return requiredDeliveryDate; }
  public void setRequiredDeliveryDate(String requiredDeliveryDate) { this.requiredDeliveryDate = requiredDeliveryDate; }

  public RequestPriority getPriority() { return priority; }
  public void setPriority(RequestPriority priority) { this.priority = priority; }

  public String getRequester() { return requester; }
  public void setRequester(String requester) { this.requester = requester; }

  public String getOrderNotes() { return orderNotes; }
  public void setOrderNotes(String orderNotes) { this.orderNotes = orderNotes; }

  public String getRequestDate() { return requestDate; }
  public void setRequestDate(String requestDate) { this.requestDate = requestDate; }

  public List<ProcurementItem> getMaterialItems() { return materialItems; }
  public void setMaterialItems(List<ProcurementItem> materialItems) { this.materialItems = materialItems; }

  public RequestStatus getStatus() { return status; }
  public void setStatus(RequestStatus status) { this.status = status; }

  public Double getTotalNetAmount() { return totalNetAmount; }
  public void setTotalNetAmount(Double totalNetAmount) { this.totalNetAmount = totalNetAmount; }

  public String getCreatedDate() { return createdDate; }
  public void setCreatedDate(String createdDate) { this.createdDate = createdDate; }

  public String getLastModifiedDate() { return lastModifiedDate; }
  public void setLastModifiedDate(String lastModifiedDate) { this.lastModifiedDate = lastModifiedDate; }

  public String getCaseUuid() { return caseUuid; }
  public void setCaseUuid(String caseUuid) { this.caseUuid = caseUuid; }
}
