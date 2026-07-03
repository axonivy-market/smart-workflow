package com.axonivy.utils.smart.workflow.governance.history.analytic.internal;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.primefaces.model.charts.bar.BarChartDataSet;
import org.primefaces.model.charts.bar.BarChartModel;
import org.primefaces.model.charts.bar.BarChartOptions;

import ch.ivyteam.ivy.environment.Ivy;

import com.axonivy.utils.smart.workflow.governance.ui.model.ChartPalette;

public class ResponseTimeChartBuilder extends AbstractChartBuilder<BarChartModel> {

  private interface BucketLabels {
    String UNDER_5S   = AbstractChartBuilder.CMS + "BucketUnder5s";
    String FROM_5_10  = AbstractChartBuilder.CMS + "Bucket5to10s";
    String FROM_10_15 = AbstractChartBuilder.CMS + "Bucket10to15s";
    String OVER_15S   = AbstractChartBuilder.CMS + "BucketOver15s";
  }

  @Override
  public BarChartModel build(HistoryAggregator stats) {
    List<String> labels = bucketLabels();
    List<Number> values = Arrays.stream(stats.getResponseTimeBuckets()).boxed().collect(Collectors.toList());

    BarChartDataSet dataSet = new BarChartDataSet();
    dataSet.setLabel(Ivy.cms().co(CMS + "DatasetSessions"));
    dataSet.setBackgroundColor(ChartPalette.PASTEL_COLORS.colors(4));
    dataSet.setData(values);

    BarChartOptions options = new BarChartOptions();
    applyResponsiveOptions(options);
    options.setIndexAxis(ChartConfig.AXIS_HORIZONTAL);
    options.setBarThickness(ChartConfig.BAR_THICKNESS);
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
