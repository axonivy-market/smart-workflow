package com.axonivy.utils.smart.workflow.governance.ui.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public enum ChartPalette {

  PASTEL_COLORS("#6299f7", "#bee3cb", "#f9908c", "#f8da96", "#8dc261", "#98bffa", "#c8befa", "#f5bf9f"),
  TWO_PASTEL_COLORS("#6299f7", "#8dc261");

  private final List<String> colors;

  ChartPalette(String... colors) {
    this.colors = Arrays.asList(colors);
  }

  public List<String> colors(int count) {
    List<String> result = new ArrayList<>(count);
    for (int i = 0; i < count; i++) {
      result.add(colors.get(i % colors.size()));
    }
    return result;
  }

  public String color(int index) {
    return colors.get(index % colors.size());
  }
}
