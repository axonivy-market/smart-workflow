package com.axonivy.utils.smart.workflow.governance.history.analytic.internal;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.primefaces.model.charts.axes.cartesian.CartesianScales;
import org.primefaces.model.charts.axes.cartesian.linear.CartesianLinearAxes;
import org.primefaces.model.charts.bar.BarChartDataSet;
import org.primefaces.model.charts.bar.BarChartModel;
import org.primefaces.model.charts.bar.BarChartOptions;

import ch.ivyteam.ivy.environment.Ivy;

import com.axonivy.utils.smart.workflow.governance.ui.model.ChartPalette;

public class TokenStackedChartBuilder extends AbstractChartBuilder<BarChartModel> {

  private static final ChartPalette PALETTE = ChartPalette.TWO_PASTEL_COLORS;

  @Override
  public BarChartModel build(HistoryAggregator stats) {
    Map<LocalDate, Long[]> byDay = prepareInputOutputByDay(stats);

    List<String> labels = new ArrayList<>();
    List<Number> inputData = new ArrayList<>();
    List<Number> outputData = new ArrayList<>();
    byDay.entrySet().stream()
        .sorted(Map.Entry.comparingByKey())
        .forEach(item -> {
          labels.add(item.getKey().format(DAY_FMT));
          inputData.add(item.getValue()[0]);
          outputData.add(item.getValue()[1]);
        });

    BarChartDataSet inputDataSet  = stackedTokenDataSet(CMS + "DatasetInputTokens",  PALETTE.color(0), inputData);
    BarChartDataSet outputDataSet = stackedTokenDataSet(CMS + "DatasetOutputTokens", PALETTE.color(1), outputData);

    BarChartOptions options = new BarChartOptions();
    applyResponsiveOptions(options);
    options.setScales(stackedTokenAxes());

    return barModel(labels, options, inputDataSet, outputDataSet);
  }

  private Map<LocalDate, Long[]> prepareInputOutputByDay(HistoryAggregator stats) {
    Map<LocalDate, Long[]> map = new LinkedHashMap<>();
    stats.getTokensByDay().forEach((date, pair) -> map.put(date, new Long[]{pair.input(), pair.output()}));
    padToMinDays(map, () -> new Long[]{0L, 0L});
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
