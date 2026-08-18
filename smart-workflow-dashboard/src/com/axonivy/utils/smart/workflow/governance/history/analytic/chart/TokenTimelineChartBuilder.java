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
import software.xdev.chartjs.model.options.LegendOptions;
import software.xdev.chartjs.model.options.Plugins;

public class TokenTimelineChartBuilder extends AbstractChartBuilder<BarChart> {

  @Override
  public BarChart build(HistoryAggregator aggregator) {
    Map<LocalDate, Long> totalsByDay = prepareTotalTokensByDay(aggregator);

    List<String> labels = new ArrayList<>();
    List<Number> values = new ArrayList<>();
    totalsByDay.forEach((date, total) -> {
      labels.add(date.format(DatePatternUtils.DAY_FMT));
      values.add(total);
    });

    BarDataset dataSet = new BarDataset();
    dataSet.setLabel(Ivy.cms().co(String.format(ANALYTICS_CMS_PATTERN, "DatasetTotalTokens")));
    dataSet.setBackgroundColor(asColors(ChartPalette.PASTEL_COLORS.colors(labels.size())));
    dataSet.setData(values);

    LegendOptions legend = new LegendOptions().setDisplay(false);

    BarOptions options = new BarOptions();
    applyResponsiveOptions(options);
    options.setScales(yIntegerScales());
    options.setPlugins(new Plugins().setLegend(legend));

    return barModel(labels, options, dataSet);
  }

  private Map<LocalDate, Long> prepareTotalTokensByDay(HistoryAggregator aggregator) {
    TreeMap<LocalDate, Long> map = new TreeMap<>();
    aggregator.getTokensByDay().forEach((date, pair) -> map.put(date, pair.input() + pair.output()));
    padTimelineToMinDays(map, () -> Long.valueOf(0));
    return map;
  }
}
