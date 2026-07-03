package com.axonivy.utils.smart.workflow.governance.history.analytic.internal;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.primefaces.model.charts.ChartData;
import org.primefaces.model.charts.ChartOptions;
import org.primefaces.model.charts.axes.cartesian.CartesianScales;
import org.primefaces.model.charts.axes.cartesian.linear.CartesianLinearAxes;
import org.primefaces.model.charts.axes.cartesian.linear.CartesianLinearTicks;
import org.primefaces.model.charts.bar.BarChartDataSet;
import org.primefaces.model.charts.bar.BarChartModel;
import org.primefaces.model.charts.bar.BarChartOptions;

abstract class AbstractChartBuilder<M> {

  abstract M build(HistoryAggregator stats);

  static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("MMM dd");
  static final String CMS = "/Dialogs/com/axonivy/utils/ai/GovernanceDashboard/Analytics/";

  interface ChartConfig {
    int    MIN_TIMELINE_DAYS = 5;
    int    TOP_N_PROCESSES   = 5;
    int    MAX_LABEL_LENGTH  = 20;
    int    BAR_THICKNESS     = 30;
    String AXIS_HORIZONTAL   = "y";
    String LEGEND_RIGHT      = "right";
    String STACK_TOKENS      = "tokens";
  }

  BarChartModel barModel(List<String> labels, BarChartOptions options, BarChartDataSet... datasets) {
    BarChartModel model = new BarChartModel();
    model.setData(chartData(labels, datasets));
    model.setOptions(options);
    return model;
  }

  static <V> void padToMinDays(Map<LocalDate, V> map, Supplier<V> zero) {
    LocalDate padFrom = map.keySet().stream().max(Comparator.naturalOrder()).orElseGet(LocalDate::now);
    while (map.size() < ChartConfig.MIN_TIMELINE_DAYS) {
      map.put(padFrom = padFrom.plusDays(1), zero.get());
    }
  }

  void applyResponsiveOptions(ChartOptions options) {
    options.setResponsive(true);
    options.setMaintainAspectRatio(false);
  }

  CartesianLinearTicks integerTicks() {
    CartesianLinearTicks ticks = new CartesianLinearTicks();
    ticks.setPrecision(0);
    return ticks;
  }

  CartesianScales xIntegerScales() {
    CartesianLinearAxes axis = new CartesianLinearAxes();
    axis.setTicks(integerTicks());
    CartesianScales scales = new CartesianScales();
    scales.addXAxesData(axis);
    return scales;
  }

  CartesianScales yIntegerScales() {
    CartesianLinearAxes axis = new CartesianLinearAxes();
    axis.setTicks(integerTicks());
    CartesianScales scales = new CartesianScales();
    scales.addYAxesData(axis);
    return scales;
  }

  String truncate(String s, int maxLen) {
    if (s == null) {
      return "";
    }
    return s.length() <= maxLen ? s : s.substring(0, maxLen - 1) + "\u2026";
  }

  private ChartData chartData(List<String> labels, BarChartDataSet... dataSets) {
    ChartData data = new ChartData();
    data.setLabels(labels);
    for (BarChartDataSet ds : dataSets) {
      data.addChartDataSet(ds);
    }
    return data;
  }
}
