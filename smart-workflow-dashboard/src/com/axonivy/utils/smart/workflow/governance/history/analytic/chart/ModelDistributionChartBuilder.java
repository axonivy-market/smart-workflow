package com.axonivy.utils.smart.workflow.governance.history.analytic.chart;

import java.util.ArrayList;
import java.util.List;

import com.axonivy.utils.smart.workflow.governance.history.analytic.chart.model.ChartPalette;

import software.xdev.chartjs.model.charts.DoughnutChart;
import software.xdev.chartjs.model.data.DoughnutData;
import software.xdev.chartjs.model.dataset.DoughnutDataset;
import software.xdev.chartjs.model.options.DoughnutOptions;
import software.xdev.chartjs.model.options.LegendOptions;
import software.xdev.chartjs.model.options.Plugins;

public class ModelDistributionChartBuilder extends AbstractChartBuilder<DoughnutChart> {

  @Override
  public DoughnutChart build(HistoryAggregator aggregator) {
    List<String> labels = new ArrayList<>();
    List<Number> values = new ArrayList<>();
    aggregator.getCountByModel().forEach((model, count) -> {
      labels.add(model);
      values.add(count);
    });

    DoughnutDataset dataSet = new DoughnutDataset();
    dataSet.setData(values);
    dataSet.setBackgroundColor(asColors(ChartPalette.PASTEL_COLORS.colors(labels.size())));

    DoughnutData data = new DoughnutData().setLabels(labels).addDataset(dataSet);

    return new DoughnutChart().setData(data).setOptions(modelDistributionOptions());
  }

  private DoughnutOptions modelDistributionOptions() {
    LegendOptions legend = new LegendOptions().setPosition(ChartConfig.LEGEND_RIGHT);
    DoughnutOptions options = new DoughnutOptions();
    applyResponsiveOptions(options);
    options.setPlugins(new Plugins().setLegend(legend));
    return options;
  }
}
