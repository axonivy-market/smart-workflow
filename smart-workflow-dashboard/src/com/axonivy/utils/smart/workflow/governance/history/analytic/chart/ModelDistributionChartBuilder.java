package com.axonivy.utils.smart.workflow.governance.history.analytic.chart;

import java.util.ArrayList;
import java.util.List;

import org.primefaces.model.charts.ChartData;
import org.primefaces.model.charts.donut.DonutChartModel;
import org.primefaces.model.charts.donut.DonutChartOptions;
import org.primefaces.model.charts.optionconfig.legend.Legend;
import org.primefaces.model.charts.pie.PieChartDataSet;

import com.axonivy.utils.smart.workflow.governance.history.analytic.chart.model.ChartPalette;

public class ModelDistributionChartBuilder extends AbstractChartBuilder<DonutChartModel> {

  @Override
  public DonutChartModel build(HistoryAggregator aggregator) {
    List<String> labels = new ArrayList<>();
    List<Number> values = new ArrayList<>();
    aggregator.getCountByModel().forEach((model, count) -> {
      labels.add(model);
      values.add(count);
    });

    PieChartDataSet dataSet = new PieChartDataSet();
    dataSet.setData(values);
    dataSet.setBackgroundColor(ChartPalette.PASTEL_COLORS.colors(labels.size()));

    ChartData data = new ChartData();
    data.setLabels(labels);
    data.addChartDataSet(dataSet);

    DonutChartModel model = new DonutChartModel();
    model.setData(data);
    model.setOptions(modelDistributionOptions());
    return model;
  }

  private DonutChartOptions modelDistributionOptions() {
    Legend legend = new Legend();
    legend.setPosition(ChartConfig.LEGEND_RIGHT);
    DonutChartOptions options = new DonutChartOptions();
    applyResponsiveOptions(options);
    options.setLegend(legend);
    return options;
  }
}
