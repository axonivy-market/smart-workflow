package com.axonivy.utils.smart.workflow.rag.tool;

import java.util.List;
import java.util.Map;
import java.util.Optional;

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
        new ToolParameter("collection", "OpenSearch index name to query.", "java.lang.String"),
        new ToolParameter("query", "The complete user question to use as the search query. Pass the full user question without modification.", "java.lang.String"),
        new ToolParameter("maxResults", "Maximum number of results to return. Uses the configured default when null.", "java.lang.Integer"),
        new ToolParameter("minScore", "Minimum similarity score threshold between 0.0 and 1.0. Uses the configured default when null.", "java.lang.Double"));
  }

  @Override
  public Object execute(Map<String, Object> args) {
    String collection = (String) args.get("collection");
    String query = (String) args.get("query");
    int maxResults = Optional.ofNullable((Integer) args.get("maxResults"))
      .orElse(-1);
    double minScore = Optional.ofNullable((Double) args.get("minScore"))
      .orElse(Double.NaN);
    return new OpenSearchRetriever().search(collection, query, maxResults, minScore);
  }
}
