package com.axonivy.utils.smart.workflow.governance.history.analytic.chart;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.primefaces.model.charts.axes.cartesian.CartesianScales;
import org.primefaces.model.charts.axes.cartesian.linear.CartesianLinearAxes;
import org.primefaces.model.charts.bar.BarChartDataSet;
import org.primefaces.model.charts.bar.BarChartModel;
import org.primefaces.model.charts.bar.BarChartOptions;

import ch.ivyteam.ivy.environment.Ivy;

import com.axonivy.utils.smart.workflow.governance.history.analytic.chart.model.ChartPalette;
import com.axonivy.utils.smart.workflow.governance.utils.DatePatternUtils;

public class TokenStackedChartBuilder extends AbstractChartBuilder<BarChartModel> {

  private static final ChartPalette PALETTE = ChartPalette.TWO_PASTEL_COLORS;

  @Override
  public BarChartModel build(HistoryAggregator aggregator) {
    Map<LocalDate, HistoryAggregator.TokenPair> byDay = prepareInputOutputByDay(aggregator);

    List<String> labels = new ArrayList<>();
    List<Number> inputData = new ArrayList<>();
    List<Number> outputData = new ArrayList<>();
    byDay.forEach((date, pair) -> {
      labels.add(date.format(DatePatternUtils.DAY_FMT));
      inputData.add(pair.input());
      outputData.add(pair.output());
    });

    BarChartDataSet inputDataSet  = stackedTokenDataSet(String.format(ANALYTICS_CMS_PATTERN, "DatasetInputTokens"),  PALETTE.color(0), inputData);
    BarChartDataSet outputDataSet = stackedTokenDataSet(String.format(ANALYTICS_CMS_PATTERN, "DatasetOutputTokens"), PALETTE.color(1), outputData);

    BarChartOptions options = new BarChartOptions();
    applyResponsiveOptions(options);
    options.setScales(stackedTokenAxes());

    return barModel(labels, options, inputDataSet, outputDataSet);
  }

  private Map<LocalDate, HistoryAggregator.TokenPair> prepareInputOutputByDay(HistoryAggregator aggregator) {
    TreeMap<LocalDate, HistoryAggregator.TokenPair> map = new TreeMap<>(aggregator.getTokensByDay());
    padTimelineToMinDays(map, () -> new HistoryAggregator.TokenPair(0L, 0L));
    return map;
  }

  private BarChartDataSet stackedTokenDataSet(String labelKey, String color, List<Number> data) {
    BarChartDataSet dataSet = new BarChartDataSet();
    dataSet.setLabel(Ivy.cms().co(labelKey));
    dataSet.setBackgroundColor(color);
    dataSet.setData(data);
    dataSet.setStack(ChartConfig.STACK_TOKENS);
    return dataSet;
  }

  private CartesianScales stackedTokenAxes() {
    CartesianLinearAxes xAxis = new CartesianLinearAxes();
    xAxis.setStacked(true);
    CartesianLinearAxes yAxis = new CartesianLinearAxes();
    yAxis.setStacked(true);
    yAxis.setTicks(integerTicks());
    CartesianScales scales = new CartesianScales();
    scales.addXAxesData(xAxis);
    scales.addYAxesData(yAxis);
    return scales;
  }
}
