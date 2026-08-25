package com.axonivy.utils.smart.workflow.governance.history.analytic.chart;

import java.util.Arrays;
import java.util.List;

import com.axonivy.utils.smart.workflow.governance.history.analytic.chart.model.ChartPalette;

import ch.ivyteam.ivy.environment.Ivy;

import software.xdev.chartjs.model.charts.BarChart;
import software.xdev.chartjs.model.dataset.BarDataset;
import software.xdev.chartjs.model.options.BarOptions;

public class ResponseTimeChartBuilder extends AbstractChartBuilder<BarChart> {

  private interface BucketLabels {
    String UNDER_5S = String.format(ANALYTICS_CMS_PATTERN, "BucketUnder5s");
    String FROM_5_10 = String.format(ANALYTICS_CMS_PATTERN, "Bucket5to10s");
    String FROM_10_15 = String.format(ANALYTICS_CMS_PATTERN, "Bucket10to15s");
    String OVER_15S = String.format(ANALYTICS_CMS_PATTERN, "BucketOver15s");
  }

  @Override
  public BarChart build(HistoryAggregator stats) {
    List<String> labels = bucketLabels();
    List<Number> values = toLongNumbers(Arrays.stream(stats.getResponseTimeBuckets()).boxed());

    BarDataset dataSet = new BarDataset();
    dataSet.setLabel(Ivy.cms().co(String.format(ANALYTICS_CMS_PATTERN, "DatasetSessions")));
    dataSet.setBackgroundColor(asColors(ChartPalette.PASTEL_COLORS.colors(labels.size())));
    dataSet.setData(values);
    dataSet.setBarThickness(ChartConfig.BAR_THICKNESS);

    BarOptions options = new BarOptions();
    applyResponsiveOptions(options);
    options.setIndexAxis(ChartConfig.AXIS_HORIZONTAL);
    options.setScales(xIntegerScales());

    return barModel(labels, options, dataSet);
  }

  private List<String> bucketLabels() {
    return List.of(
        Ivy.cms().co(BucketLabels.UNDER_5S),
        Ivy.cms().co(BucketLabels.FROM_5_10),
        Ivy.cms().co(BucketLabels.FROM_10_15),
        Ivy.cms().co(BucketLabels.OVER_15S));
  }
}
