package com.axonivy.utils.smart.workflow.governance.history.analytic.internal;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.primefaces.model.charts.bar.BarChartDataSet;
import org.primefaces.model.charts.bar.BarChartModel;
import org.primefaces.model.charts.bar.BarChartOptions;

import ch.ivyteam.ivy.environment.Ivy;

import com.axonivy.utils.smart.workflow.governance.ui.model.ChartPalette;

public class TopCasesChartBuilder extends AbstractChartBuilder<BarChartModel> {

  @Override
  public BarChartModel build(HistoryAggregator stats) {
    String unknownLabel = Ivy.cms().co(CMS + "UnknownProcess");
    List<Map.Entry<String, Long>> sorted = topCasesEntries(stats);

    List<String> labels = sorted.stream()
        .map(item -> item.getKey().isEmpty() ? unknownLabel : item.getKey())
        .collect(Collectors.toList());
    List<Number> values = sorted.stream().map(Map.Entry::getValue).collect(Collectors.toList());

    BarChartDataSet dataSet = new BarChartDataSet();
    dataSet.setLabel(Ivy.cms().co(CMS + "DatasetTotalTokens"));
    dataSet.setBackgroundColor(ChartPalette.PASTEL_COLORS.colors(labels.size()));
    dataSet.setData(values);

    BarChartOptions options = new BarChartOptions();
    applyResponsiveOptions(options);
    options.setIndexAxis(ChartConfig.AXIS_HORIZONTAL);
    options.setScales(xIntegerScales());

    return barModel(labels, options, dataSet);
  }

  private List<Map.Entry<String, Long>> topCasesEntries(HistoryAggregator stats) {
    return stats.getTokensByProcess().entrySet().stream()
        .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
        .limit(ChartConfig.TOP_N_PROCESSES)
        .collect(Collectors.toList());
  }
}
