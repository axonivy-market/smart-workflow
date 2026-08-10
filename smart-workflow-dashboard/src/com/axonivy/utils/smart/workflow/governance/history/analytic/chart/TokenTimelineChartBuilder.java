package com.axonivy.utils.smart.workflow.governance.history.analytic.chart;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.primefaces.model.charts.bar.BarChartDataSet;
import org.primefaces.model.charts.bar.BarChartModel;
import org.primefaces.model.charts.bar.BarChartOptions;
import org.primefaces.model.charts.optionconfig.legend.Legend;

import ch.ivyteam.ivy.environment.Ivy;

import com.axonivy.utils.smart.workflow.governance.history.analytic.chart.model.ChartPalette;
import com.axonivy.utils.smart.workflow.governance.utils.DatePatternUtils;

public class TokenTimelineChartBuilder extends AbstractChartBuilder<BarChartModel> {

  @Override
  public BarChartModel build(HistoryAggregator aggregator) {
    Map<LocalDate, Long> totalsByDay = prepareTotalTokensByDay(aggregator);

    List<String> labels = new ArrayList<>();
    List<Number> values = new ArrayList<>();
    totalsByDay.forEach((date, total) -> {
      labels.add(date.format(DatePatternUtils.DAY_FMT));
      values.add(total);
    });

    BarChartDataSet dataSet = new BarChartDataSet();
    dataSet.setLabel(Ivy.cms().co(String.format(ANALYTICS_CMS_PATTERN, "DatasetTotalTokens")));
    dataSet.setBackgroundColor(ChartPalette.PASTEL_COLORS.colors(labels.size()));
    dataSet.setData(values);

    Legend legend = new Legend();
    legend.setDisplay(false);

    BarChartOptions options = new BarChartOptions();
    applyResponsiveOptions(options);
    options.setScales(yIntegerScales());
    options.setLegend(legend);

    return barModel(labels, options, dataSet);
  }

  private Map<LocalDate, Long> prepareTotalTokensByDay(HistoryAggregator aggregator) {
    TreeMap<LocalDate, Long> map = new TreeMap<>();
    aggregator.getTokensByDay().forEach((date, pair) -> map.put(date, pair.input() + pair.output()));
    padTimelineToMinDays(map, () -> Long.valueOf(0));
    return map;
  }
}
