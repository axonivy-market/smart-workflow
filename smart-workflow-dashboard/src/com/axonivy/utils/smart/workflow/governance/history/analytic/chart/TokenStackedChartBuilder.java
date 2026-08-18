package com.axonivy.utils.smart.workflow.governance.history.analytic.chart;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.axonivy.utils.smart.workflow.governance.history.analytic.chart.model.ChartPalette;
import com.axonivy.utils.smart.workflow.governance.utils.DatePatternUtils;

import ch.ivyteam.ivy.environment.Ivy;

import software.xdev.chartjs.model.charts.BarChart;
import software.xdev.chartjs.model.dataset.BarDataset;
import software.xdev.chartjs.model.options.BarOptions;
import software.xdev.chartjs.model.options.scale.Scales;
import software.xdev.chartjs.model.options.scale.cartesian.category.CategoryScaleOptions;
import software.xdev.chartjs.model.options.scale.cartesian.linear.LinearScaleOptions;

public class TokenStackedChartBuilder extends AbstractChartBuilder<BarChart> {

  private static final ChartPalette PALETTE = ChartPalette.TWO_PASTEL_COLORS;

  @Override
  public BarChart build(HistoryAggregator aggregator) {
    Map<LocalDate, HistoryAggregator.TokenPair> byDay = prepareInputOutputByDay(aggregator);

    List<String> labels = new ArrayList<>();
    List<Number> inputData = new ArrayList<>();
    List<Number> outputData = new ArrayList<>();
    byDay.forEach((date, pair) -> {
      labels.add(date.format(DatePatternUtils.DAY_FMT));
      inputData.add(pair.input());
      outputData.add(pair.output());
    });

    BarDataset inputDataSet  = stackedTokenDataSet(String.format(ANALYTICS_CMS_PATTERN, "DatasetInputTokens"),  PALETTE.color(0), inputData);
    BarDataset outputDataSet = stackedTokenDataSet(String.format(ANALYTICS_CMS_PATTERN, "DatasetOutputTokens"), PALETTE.color(1), outputData);

    BarOptions options = new BarOptions();
    applyResponsiveOptions(options);
    options.setScales(stackedTokenAxes());

    return barModel(labels, options, inputDataSet, outputDataSet);
  }

  private Map<LocalDate, HistoryAggregator.TokenPair> prepareInputOutputByDay(HistoryAggregator aggregator) {
    TreeMap<LocalDate, HistoryAggregator.TokenPair> map = new TreeMap<>(aggregator.getTokensByDay());
    padTimelineToMinDays(map, () -> new HistoryAggregator.TokenPair(0L, 0L));
    return map;
  }

  private BarDataset stackedTokenDataSet(String labelKey, String color, List<Number> data) {
    BarDataset dataSet = new BarDataset();
    dataSet.setLabel(Ivy.cms().co(labelKey));
    dataSet.setBackgroundColor(color);
    dataSet.setData(data);
    dataSet.setStack(ChartConfig.STACK_TOKENS);
    return dataSet;
  }

  private Scales stackedTokenAxes() {
    CategoryScaleOptions xAxis = new CategoryScaleOptions();
    xAxis.setStacked(true);
    LinearScaleOptions yAxis = new LinearScaleOptions();
    yAxis.setStacked(true);
    yAxis.setTicks(integerTicks());
    return new Scales()
        .addScale(Scales.ScaleAxis.X, xAxis)
        .addScale(Scales.ScaleAxis.Y, yAxis);
  }
}
