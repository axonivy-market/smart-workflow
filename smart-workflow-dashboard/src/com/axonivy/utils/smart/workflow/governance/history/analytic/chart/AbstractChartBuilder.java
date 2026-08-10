package com.axonivy.utils.smart.workflow.governance.history.analytic.chart;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.NavigableMap;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.primefaces.model.charts.ChartData;
import org.primefaces.model.charts.ChartOptions;
import org.primefaces.model.charts.axes.cartesian.CartesianScales;
import org.primefaces.model.charts.axes.cartesian.linear.CartesianLinearAxes;
import org.primefaces.model.charts.axes.cartesian.linear.CartesianLinearTicks;
import org.primefaces.model.charts.bar.BarChartDataSet;
import org.primefaces.model.charts.bar.BarChartModel;
import org.primefaces.model.charts.bar.BarChartOptions;

abstract class AbstractChartBuilder<M> {

  public abstract M build(HistoryAggregator aggregator);

  protected static final String ANALYTICS_CMS_PATTERN = "/Dialogs/com/axonivy/utils/ai/GovernanceDashboard/Analytics/%s";

  interface ChartConfig {
    int    MIN_TIMELINE_DAYS = 5;
    int    TOP_N_PROCESSES   = 5;
    int    BAR_THICKNESS     = 30;
    String AXIS_HORIZONTAL   = "y";
    String LEGEND_RIGHT      = "right";
    String STACK_TOKENS      = "tokens";
  }

  protected BarChartModel barModel(List<String> labels, BarChartOptions options, BarChartDataSet... datasets) {
    BarChartModel model = new BarChartModel();
    model.setData(chartData(labels, datasets));
    model.setOptions(options);
    return model;
  }

  protected static List<Number> toLongNumbers(Stream<Long> values) {
    return values.map(Number.class::cast).toList();
  }

  // mutates map: fills gaps and pads to MIN_TIMELINE_DAYS
  protected static <V> void padTimelineToMinDays(NavigableMap<LocalDate, V> map, Supplier<V> emptyValueSupplier) {
    LocalDate lastDate;
    if (!map.isEmpty()) {
      LocalDate min = map.firstKey();
      lastDate = map.lastKey();
      for (LocalDate d = min; !d.isAfter(lastDate); d = d.plusDays(1)) {
        map.putIfAbsent(d, emptyValueSupplier.get());
      }
    } else {
      lastDate = LocalDate.now();
    }
    while (map.size() < ChartConfig.MIN_TIMELINE_DAYS) {
      map.put(lastDate = lastDate.plusDays(1), emptyValueSupplier.get());
    }
  }

  protected void applyResponsiveOptions(ChartOptions options) {
    options.setResponsive(true);
    options.setMaintainAspectRatio(false);
  }

  protected CartesianLinearTicks integerTicks() {
    CartesianLinearTicks ticks = new CartesianLinearTicks();
    ticks.setPrecision(0);
    return ticks;
  }

  protected CartesianScales xIntegerScales() {
    CartesianScales scales = new CartesianScales();
    scales.addXAxesData(integerAxis());
    return scales;
  }

  protected CartesianScales yIntegerScales() {
    CartesianScales scales = new CartesianScales();
    scales.addYAxesData(integerAxis());
    return scales;
  }

  private CartesianLinearAxes integerAxis() {
    CartesianLinearAxes axis = new CartesianLinearAxes();
    axis.setTicks(integerTicks());
    return axis;
  }

  private ChartData chartData(List<String> labels, BarChartDataSet... dataSets) {
    ChartData data = new ChartData();
    data.setLabels(labels);
    Arrays.stream(dataSets).forEach(data::addChartDataSet);
    return data;
  }
}
