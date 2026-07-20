package com.axonivy.utils.smart.workflow.governance.history.analytic.internal;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.primefaces.model.charts.bar.BarChartDataSet;
import org.primefaces.model.charts.bar.BarChartModel;
import org.primefaces.model.charts.bar.BarChartOptions;
import org.primefaces.model.charts.optionconfig.legend.Legend;

import ch.ivyteam.ivy.environment.Ivy;

import com.axonivy.utils.smart.workflow.governance.ui.model.ChartPalette;
import com.axonivy.utils.smart.workflow.governance.utils.DatePatternUtils;

public class TokenTimelineChartBuilder extends AbstractChartBuilder<BarChartModel> {

  @Override
  public BarChartModel build(HistoryAggregator stats) {
    Map<LocalDate, Long> totalsByDay = prepareTotalTokensByDay(stats);

    List<String> labels = new ArrayList<>();
    List<Number> values = new ArrayList<>();
    totalsByDay.entrySet().stream()
        .sorted(Map.Entry.comparingByKey())
        .forEach(item -> {
          labels.add(item.getKey().format(DatePatternUtils.DAY_FMT));
          values.add(item.getValue());
        });

    BarChartDataSet dataSet = new BarChartDataSet();
    dataSet.setLabel(Ivy.cms().co(CMS + "DatasetTotalTokens"));
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

  private Map<LocalDate, Long> prepareTotalTokensByDay(HistoryAggregator stats) {
    Map<LocalDate, Long> map = new LinkedHashMap<>();
    stats.getTokensByDay().forEach((date, pair) -> map.put(date, pair.input() + pair.output()));
    padToMinDays(map, () -> 0L);
    return map;
  }
}
