package com.axonivy.utils.smart.workflow.demo.erp.procurement.agent.feedback;

import dev.langchain4j.model.output.structured.Description;

public class FeedbackOption {
    @Description("ID of the option.")
    private String optionId;

    @Description("Description of the option.")
    private String description;

    public String getOptionId() { return optionId; }
    public void setOptionId(String optionId) { this.optionId = optionId; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
