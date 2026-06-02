package com.axonivy.utils.smart.workflow.demo.erp.procurement.bean;

import java.io.Serializable;
import java.util.Optional;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

import com.axonivy.utils.smart.workflow.demo.erp.procurement.mock.ProcurementMockDataGenerator;
import com.axonivy.utils.smart.workflow.demo.erp.procurement.repository.MaterialTypeRepository;

import ch.ivyteam.ivy.cm.ContentObject;
import ch.ivyteam.ivy.cm.ContentObjectValue;
import ch.ivyteam.ivy.environment.Ivy;

@ManagedBean
@ViewScoped
public class ProcurementStartDemoBean implements Serializable {

  private static final long serialVersionUID = 1L;

  private String step1Status = "pending";
  private boolean generationStarted = false;
  private boolean generationDone = false;

  public boolean isDataAlreadyGenerated() {
    return !MaterialTypeRepository.getInstance().findAll().isEmpty();
  }

  public void generateMockData() {
    generationStarted = true;
    step1Status = "running";
    try {
      ProcurementMockDataGenerator.create();
      step1Status = "completed";
    } catch (Exception e) {
      step1Status = "failed";
      Ivy.log().error("Procurement mock data generation failed", e);
    }
    generationDone = step1Status.equals("completed");
  }

  public String getStepClass(String status) {
    return switch (status) {
      case "running"   -> "pr-checklist-item running pr-tl-item";
      case "completed" -> "pr-checklist-item completed pr-tl-item";
      case "failed"    -> "pr-checklist-item failed pr-tl-item";
      default          -> "pr-checklist-item pending pr-tl-item";
    };
  }

  public String getBubbleClass(String status) {
    return switch (status) {
      case "running"   -> "pr-tl-bubble pr-tl-bubble-running";
      case "completed" -> "pr-tl-bubble pr-tl-bubble-completed";
      case "failed"    -> "pr-tl-bubble pr-tl-bubble-failed";
      default          -> "pr-tl-bubble pr-tl-bubble-pending";
    };
  }

  public String getStatusIcon(String status) {
    return switch (status) {
      case "running"   -> "ti ti-loader pr-spin";
      case "completed" -> "ti ti-circle-check";
      case "failed"    -> "ti ti-circle-x";
      default          -> "ti ti-clock";
    };
  }

  public StreamedContent downloadMockPdf() {
    Optional<ContentObject> obj = Ivy.cm().findObject("/Files/ERP/Procurement/ProcurementRequest");
    if (obj.map(ContentObject::exists).orElse(false)) {
      ContentObjectValue cov = obj.map(ContentObject::values).map(v -> v.getFirst()).orElse(null);
      if (cov != null) {
        return DefaultStreamedContent.builder()
            .name("MockProcurementRequest.pdf")
            .contentType("application/pdf")
            .stream(() -> cov.read().inputStream())
            .build();
      }
    }
    Ivy.log().warn("CMS file not found: /Files/ERP/Procurement/ProcurementRequest");
    return DefaultStreamedContent.builder()
        .name("MockProcurementRequest.pdf")
        .contentType("application/pdf")
        .stream(() -> new java.io.ByteArrayInputStream(new byte[0]))
        .build();
  }

  public String getStep1Status() { return step1Status; }
  public boolean isGenerationStarted() { return generationStarted; }
  public boolean isGenerationDone() { return generationDone; }
}
