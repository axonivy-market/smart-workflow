package com.axonivy.utils.smart.workflow.rag.tool;

import java.util.List;
import java.util.Map;

import com.axonivy.utils.smart.workflow.rag.pipeline.internal.OpenSearchRetriever;
import com.axonivy.utils.smart.workflow.tools.provider.SmartWorkflowTool;

public class OpenSearchRagTool implements SmartWorkflowTool {

  @Override
  public String name() {
    return "openSearchSearch";
  }

  @Override
  public String description() {
    return "Search a pre-indexed OpenSearch knowledge base using semantic similarity.";
  }

  @Override
  public List<ToolParameter> parameters() {
    return List.of(
        new ToolParameter("collection", "OpenSearch index name to query. Uses the configured default when blank.", "String"),
        new ToolParameter("query", "The search query to find relevant content", "String"),
        new ToolParameter("maxResults", "Maximum number of results to return. Uses the configured default when null.", "Integer"),
        new ToolParameter("minScore", "Minimum similarity score threshold between 0.0 and 1.0. Uses the configured default when null.", "Double"));
  }

  @Override
  public Object execute(Map<String, Object> args) {
    String collection = (String) args.get("collection");
    String query = (String) args.get("query");
    Integer maxResults = (Integer) args.get("maxResults");
    Double minScore = (Double) args.get("minScore");
    return new OpenSearchRetriever().search(collection, query, maxResults, minScore);
  }
}
