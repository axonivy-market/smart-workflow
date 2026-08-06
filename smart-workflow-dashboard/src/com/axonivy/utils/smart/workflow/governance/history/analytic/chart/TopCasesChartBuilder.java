package com.axonivy.utils.smart.workflow.governance.history.analytic.chart;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.primefaces.model.charts.bar.BarChartDataSet;
import org.primefaces.model.charts.bar.BarChartModel;
import org.primefaces.model.charts.bar.BarChartOptions;

import com.axonivy.utils.smart.workflow.governance.history.analytic.chart.model.ChartPalette;

import ch.ivyteam.ivy.environment.Ivy;

public class TopCasesChartBuilder extends AbstractChartBuilder<BarChartModel> {

  @Override
  public BarChartModel build(HistoryAggregator aggregator) {
    String unknownLabel = Ivy.cms().co(String.format(ANALYTICS_CMS_PATTERN, "UnknownProcess"));
    List<Map.Entry<String, Long>> sorted = topCasesEntries(aggregator);

    List<String> labels = new ArrayList<>();
    List<Number> values = new ArrayList<>();
    sorted.forEach(item -> {
      labels.add(item.getKey().isEmpty() ? unknownLabel : item.getKey());
      values.add(item.getValue());
    });

    BarChartDataSet dataSet = new BarChartDataSet();
    dataSet.setLabel(Ivy.cms().co(String.format(ANALYTICS_CMS_PATTERN, "DatasetTotalTokens")));
    dataSet.setBackgroundColor(ChartPalette.PASTEL_COLORS.colors(labels.size()));
    dataSet.setData(values);

    BarChartOptions options = new BarChartOptions();
    applyResponsiveOptions(options);
    options.setIndexAxis(ChartConfig.AXIS_HORIZONTAL);
    options.setScales(xIntegerScales());

    return barModel(labels, options, dataSet);
  }

  private List<Map.Entry<String, Long>> topCasesEntries(HistoryAggregator aggregator) {
    return aggregator.getTokensByProcess().entrySet().stream()
        .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
        .limit(ChartConfig.TOP_N_PROCESSES)
        .toList();
  }
}
