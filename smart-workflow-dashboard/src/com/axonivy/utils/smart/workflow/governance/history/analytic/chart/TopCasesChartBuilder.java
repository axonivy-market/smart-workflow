package com.axonivy.utils.smart.workflow.governance.history.analytic.chart;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.axonivy.utils.smart.workflow.governance.history.analytic.chart.model.ChartPalette;

import ch.ivyteam.ivy.environment.Ivy;

import software.xdev.chartjs.model.charts.BarChart;
import software.xdev.chartjs.model.dataset.BarDataset;
import software.xdev.chartjs.model.options.BarOptions;

public class TopCasesChartBuilder extends AbstractChartBuilder<BarChart> {

  @Override
  public BarChart build(HistoryAggregator aggregator) {
    String unknownLabel = Ivy.cms().co(String.format(ANALYTICS_CMS_PATTERN, "UnknownProcess"));
    List<Map.Entry<String, Long>> sorted = topCasesEntries(aggregator);

    List<String> labels = new ArrayList<>();
    List<Number> values = new ArrayList<>();
    sorted.forEach(item -> {
      labels.add(item.getKey().isEmpty() ? unknownLabel : item.getKey());
      values.add(item.getValue());
    });

    BarDataset dataSet = new BarDataset();
    dataSet.setLabel(Ivy.cms().co(String.format(ANALYTICS_CMS_PATTERN, "DatasetTotalTokens")));
    dataSet.setBackgroundColor(asColors(ChartPalette.PASTEL_COLORS.colors(labels.size())));
    dataSet.setData(values);

    BarOptions options = new BarOptions();
    applyResponsiveOptions(options);
    options.setIndexAxis(ChartConfig.AXIS_HORIZONTAL);
    options.setScales(xIntegerScales());

    return barModel(labels, options, dataSet);
  }

  private List<Map.Entry<String, Long>> topCasesEntries(HistoryAggregator aggregator) {
    return aggregator.getTokensByProcess().entrySet().stream()
        .sorted(Map.Entry.<String, Long> comparingByValue().reversed())
        .limit(ChartConfig.TOP_N_PROCESSES)
        .toList();
  }
}
