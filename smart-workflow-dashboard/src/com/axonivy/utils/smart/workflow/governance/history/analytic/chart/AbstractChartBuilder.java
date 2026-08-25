package com.axonivy.utils.smart.workflow.governance.history.analytic.chart;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NavigableMap;
import java.util.function.Supplier;
import java.util.stream.Stream;

import software.xdev.chartjs.model.charts.BarChart;
import software.xdev.chartjs.model.data.BarData;
import software.xdev.chartjs.model.dataset.BarDataset;
import software.xdev.chartjs.model.enums.IndexAxis;
import software.xdev.chartjs.model.options.BarOptions;
import software.xdev.chartjs.model.options.Options;
import software.xdev.chartjs.model.options.scale.Scales;
import software.xdev.chartjs.model.options.scale.cartesian.linear.LinearScaleOptions;
import software.xdev.chartjs.model.options.scale.cartesian.linear.LinearTickOptions;

abstract class AbstractChartBuilder<M> {

  public abstract M build(HistoryAggregator aggregator);

  protected static final String ANALYTICS_CMS_PATTERN = "/Dialogs/com/axonivy/utils/ai/GovernanceDashboard/Analytics/%s";

  interface ChartConfig {
    int       MIN_TIMELINE_DAYS = 5;
    int       TOP_N_PROCESSES   = 5;
    int       BAR_THICKNESS     = 30;
    IndexAxis AXIS_HORIZONTAL   = IndexAxis.Y;
    String    LEGEND_RIGHT      = "right";
    String    STACK_TOKENS      = "tokens";
  }

  protected BarChart barModel(List<String> labels, BarOptions options, BarDataset... datasets) {
    BarData data = new BarData().setLabels(labels).setDatasets(Arrays.asList(datasets));
    return new BarChart().setData(data).setOptions(options);
  }

  protected static List<Number> toLongNumbers(Stream<Long> values) {
    return values.map(Number.class::cast).toList();
  }

  protected static List<Object> asColors(List<String> hexColors) {
    return new ArrayList<>(hexColors);
  }

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

  protected void applyResponsiveOptions(Options<?, ?> options) {
    options.setResponsive(true);
    options.setMaintainAspectRatio(false);
  }

  protected LinearTickOptions integerTicks() {
    return new LinearTickOptions().setPrecision(0);
  }

  protected Scales xIntegerScales() {
    return new Scales().addScale(Scales.ScaleAxis.X, new LinearScaleOptions().setTicks(integerTicks()));
  }

  protected Scales yIntegerScales() {
    return new Scales().addScale(Scales.ScaleAxis.Y, new LinearScaleOptions().setTicks(integerTicks()));
  }
}
