package com.axonivy.utils.smart.workflow.governance.history.analytic.internal;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.primefaces.model.charts.ChartData;
import org.primefaces.model.charts.ChartOptions;
import org.primefaces.model.charts.axes.cartesian.CartesianScales;
import org.primefaces.model.charts.axes.cartesian.linear.CartesianLinearAxes;
import org.primefaces.model.charts.axes.cartesian.linear.CartesianLinearTicks;
import org.primefaces.model.charts.bar.BarChartDataSet;
import org.primefaces.model.charts.bar.BarChartModel;
import org.primefaces.model.charts.bar.BarChartOptions;
import org.primefaces.model.charts.donut.DonutChartModel;
import org.primefaces.model.charts.donut.DonutChartOptions;
import org.primefaces.model.charts.optionconfig.legend.Legend;
import org.primefaces.model.charts.pie.PieChartDataSet;

import ch.ivyteam.ivy.environment.Ivy;

import com.axonivy.utils.smart.workflow.governance.history.entity.AgentConversationEntry;
import com.axonivy.utils.smart.workflow.governance.ui.model.ChartPalette;
import com.axonivy.utils.smart.workflow.governance.utils.ChatHistoryJsonParser;

public class HistoryChartBuilder {

  private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("MMM dd");

  private static final String CMS = "/Dialogs/com/axonivy/utils/ai/component/Analytics/Analytics/";

  // ── Chart palette assignments — restyle any chart by changing one line ────
  private static final ChartPalette PALETTE_TOKEN_TIMELINE = ChartPalette.PASTEL_COLORS;
  private static final ChartPalette PALETTE_MODEL_DIST     = ChartPalette.PASTEL_COLORS;
  private static final ChartPalette PALETTE_TOKEN_STACKED  = ChartPalette.TWO_PASTEL_COLORS;
  private static final ChartPalette PALETTE_TOP_CASES      = ChartPalette.PASTEL_COLORS;
  private static final ChartPalette PALETTE_RESPONSE_TIME  = ChartPalette.PASTEL_COLORS;

  private static final int    MIN_TIMELINE_DAYS    = 5;
  private static final int    TOP_N_PROCESSES      = 5;
  private static final int    MAX_LABEL_LENGTH     = 20;
  private static final int    HISTOGRAM_BAR_THICK  = 30;
  private static final String AXIS_HORIZONTAL      = "y";
  private static final String LEGEND_RIGHT         = "right";
  private static final String STACK_TOKENS         = "tokens";
  private static final long   MS_BUCKET_FAST       = 5_000;
  private static final long   MS_BUCKET_MEDIUM     = 10_000;
  private static final long   MS_BUCKET_SLOW       = 15_000;

  // ── Chart A: Token Usage Over Time (bar) ─────────────────────────────────

  public BarChartModel buildTokenTimeline(List<AgentConversationEntry> entries) {
    Map<LocalDate, Long> tokensByDate = new LinkedHashMap<>();
    entries.stream()
        .filter(entry -> entry.getLastUpdated() != null)
        .sorted((left, right) -> left.getLastUpdated().compareTo(right.getLastUpdated()))
        .forEach(entry -> {
          try {
            LocalDate date = LocalDateTime.parse(entry.getLastUpdated()).toLocalDate();
            tokensByDate.merge(date, (long) ChatHistoryJsonParser.getTotalTokens(entry), Long::sum);
          } catch (DateTimeParseException ex) {
            Ivy.log().warn("Skipping entry with unparseable date ''{0}'': {1}",
                entry.getLastUpdated(), ex.getMessage());
          }
        });

    LocalDate padFrom = tokensByDate.keySet().stream()
        .max(Comparator.naturalOrder())
        .orElseGet(LocalDate::now);
    while (tokensByDate.size() < MIN_TIMELINE_DAYS) {
      tokensByDate.put(padFrom = padFrom.plusDays(1), 0L);
    }

    List<String> labels = new ArrayList<>();
    List<Number> values = new ArrayList<>();
    tokensByDate.entrySet().stream()
        .sorted(Map.Entry.comparingByKey())
        .forEach(item -> {
          labels.add(item.getKey().format(DAY_FMT));
          values.add(item.getValue());
        });

    BarChartDataSet dataSet = new BarChartDataSet();
    dataSet.setLabel(Ivy.cms().co(CMS + "DatasetTotalTokens"));
    dataSet.setBackgroundColor(PALETTE_TOKEN_TIMELINE.colors(labels.size()));
    dataSet.setData(values);

    ChartData data = new ChartData();
    data.setLabels(labels);
    data.addChartDataSet(dataSet);

    CartesianLinearAxes yAxis = new CartesianLinearAxes();
    yAxis.setTicks(integerTicks());

    CartesianScales scales = new CartesianScales();
    scales.addYAxesData(yAxis);

    Legend legend = new Legend();
    legend.setDisplay(false);

    BarChartOptions options = new BarChartOptions();
    applyResponsiveOptions(options);
    options.setScales(scales);
    options.setLegend(legend);

    BarChartModel model = new BarChartModel();
    model.setData(data);
    model.setOptions(options);
    return model;
  }

  // ── Chart B: Model Distribution (donut) ──────────────────────────────────

  public DonutChartModel buildModelDistribution(List<AgentConversationEntry> entries) {
    Map<String, Long> counts = entries.stream()
        .collect(Collectors.groupingBy(ChatHistoryJsonParser::getModelName, Collectors.counting()));

    List<Map.Entry<String, Long>> countEntries = new ArrayList<>(counts.entrySet());
    List<String> labels = countEntries.stream().map(Map.Entry::getKey).collect(Collectors.toList());
    List<Number> values = countEntries.stream().map(Map.Entry::getValue).collect(Collectors.toList());

    PieChartDataSet dataSet = new PieChartDataSet();
    dataSet.setData(values);
    dataSet.setBackgroundColor(PALETTE_MODEL_DIST.colors(labels.size()));

    ChartData data = new ChartData();
    data.setLabels(labels);
    data.addChartDataSet(dataSet);

    Legend legend = new Legend();
    legend.setPosition(LEGEND_RIGHT);

    DonutChartOptions options = new DonutChartOptions();
    applyResponsiveOptions(options);
    options.setLegend(legend);

    DonutChartModel model = new DonutChartModel();
    model.setData(data);
    model.setOptions(options);
    return model;
  }

  // ── Chart C: Input vs Output per Day (stacked bar) ───────────────────────

  public BarChartModel buildTokenStacked(List<AgentConversationEntry> entries) {
    Map<LocalDate, Long[]> byDay = new LinkedHashMap<>();
    entries.stream()
        .filter(entry -> entry.getLastUpdated() != null)
        .sorted((left, right) -> left.getLastUpdated().compareTo(right.getLastUpdated()))
        .forEach(entry -> {
          try {
            LocalDate date = LocalDateTime.parse(entry.getLastUpdated()).toLocalDate();
            byDay.merge(date,
                new Long[]{ChatHistoryJsonParser.getInputTokens(entry),
                           ChatHistoryJsonParser.getOutputTokens(entry)},
                (existing, incoming) -> new Long[]{
                    existing[0] + incoming[0], existing[1] + incoming[1]});
          } catch (DateTimeParseException ex) {
            Ivy.log().warn("Skipping entry with unparseable date ''{0}'': {1}",
                entry.getLastUpdated(), ex.getMessage());
          }
        });

    LocalDate padFrom = byDay.keySet().stream()
        .max(Comparator.naturalOrder())
        .orElseGet(LocalDate::now);
    while (byDay.size() < MIN_TIMELINE_DAYS) {
      byDay.put(padFrom = padFrom.plusDays(1), new Long[]{0L, 0L});
    }

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

    BarChartDataSet inputDataSet = new BarChartDataSet();
    inputDataSet.setLabel(Ivy.cms().co(CMS + "DatasetInputTokens"));
    inputDataSet.setBackgroundColor(PALETTE_TOKEN_STACKED.color(0));
    inputDataSet.setData(inputData);
    inputDataSet.setStack(STACK_TOKENS);

    BarChartDataSet outputDataSet = new BarChartDataSet();
    outputDataSet.setLabel(Ivy.cms().co(CMS + "DatasetOutputTokens"));
    outputDataSet.setBackgroundColor(PALETTE_TOKEN_STACKED.color(1));
    outputDataSet.setData(outputData);
    outputDataSet.setStack(STACK_TOKENS);

    CartesianLinearAxes xAxis = new CartesianLinearAxes();
    xAxis.setStacked(true);
    CartesianLinearAxes yAxis = new CartesianLinearAxes();
    yAxis.setStacked(true);
    yAxis.setTicks(integerTicks());

    CartesianScales scales = new CartesianScales();
    scales.addXAxesData(xAxis);
    scales.addYAxesData(yAxis);

    BarChartOptions options = new BarChartOptions();
    applyResponsiveOptions(options);
    options.setScales(scales);

    BarChartModel model = new BarChartModel();
    model.setData(data(labels, inputDataSet, outputDataSet));
    model.setOptions(options);
    return model;
  }

  // ── Chart D: Top 5 Processes by Token Usage (horizontal bar) ─────────────

  public BarChartModel buildTopCases(List<AgentConversationEntry> entries) {
    Map<String, Long> byCase = entries.stream()
        .collect(Collectors.groupingBy(
            entry -> entry.getProcessName() != null && !entry.getProcessName().isEmpty()
                ? entry.getProcessName() : Ivy.cms().co(CMS + "UnknownProcess"),
            Collectors.summingLong(ChatHistoryJsonParser::getTotalTokens)));

    List<Map.Entry<String, Long>> sorted = byCase.entrySet().stream()
        .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
        .limit(TOP_N_PROCESSES)
        .collect(Collectors.toList());

    List<String> labels = sorted.stream()
        .map(item -> truncate(item.getKey(), MAX_LABEL_LENGTH))
        .collect(Collectors.toList());
    List<Number> values = sorted.stream()
        .map(Map.Entry::getValue)
        .collect(Collectors.toList());

    BarChartDataSet dataSet = new BarChartDataSet();
    dataSet.setLabel(Ivy.cms().co(CMS + "DatasetTotalTokens"));
    dataSet.setBackgroundColor(PALETTE_TOP_CASES.colors(labels.size()));
    dataSet.setData(values);

    CartesianLinearAxes xAxis = new CartesianLinearAxes();
    xAxis.setTicks(integerTicks());

    CartesianScales scales = new CartesianScales();
    scales.addXAxesData(xAxis);

    BarChartOptions options = new BarChartOptions();
    applyResponsiveOptions(options);
    options.setIndexAxis(AXIS_HORIZONTAL);
    options.setScales(scales);

    BarChartModel model = new BarChartModel();
    model.setData(data(labels, dataSet));
    model.setOptions(options);
    return model;
  }

  // ── Chart E: Response Time Distribution (horizontal histogram) ────────────

  public BarChartModel buildResponseTimeHistogram(List<AgentConversationEntry> entries) {
    long[] buckets = new long[4];
    for (AgentConversationEntry entry : entries) {
      long ms = ChatHistoryJsonParser.getAvgDurationMs(entry);
      if (ms < MS_BUCKET_FAST) {
        buckets[0]++;
      } else if (ms < MS_BUCKET_MEDIUM) {
        buckets[1]++;
      } else if (ms < MS_BUCKET_SLOW) {
        buckets[2]++;
      } else {
        buckets[3]++;
      }
    }

    List<String> labels = List.of(
        Ivy.cms().co(CMS + "BucketUnder5s"),
        Ivy.cms().co(CMS + "Bucket5to10s"),
        Ivy.cms().co(CMS + "Bucket10to15s"),
        Ivy.cms().co(CMS + "BucketOver15s"));
    List<Number> values = new ArrayList<>();
    for (long v : buckets) {
      values.add(v);
    }

    BarChartDataSet dataSet = new BarChartDataSet();
    dataSet.setLabel(Ivy.cms().co(CMS + "DatasetSessions"));
    dataSet.setBackgroundColor(PALETTE_RESPONSE_TIME.colors(4));
    dataSet.setData(values);

    CartesianLinearAxes xAxis = new CartesianLinearAxes();
    xAxis.setTicks(integerTicks());

    CartesianScales scales = new CartesianScales();
    scales.addXAxesData(xAxis);

    BarChartOptions options = new BarChartOptions();
    applyResponsiveOptions(options);
    options.setIndexAxis(AXIS_HORIZONTAL);
    options.setBarThickness(HISTOGRAM_BAR_THICK);
    options.setScales(scales);

    BarChartModel model = new BarChartModel();
    model.setData(data(labels, dataSet));
    model.setOptions(options);
    return model;
  }

  // ── Helpers ───────────────────────────────────────────────────────────────

  private ChartData data(List<String> labels, BarChartDataSet... dataSets) {
    ChartData data = new ChartData();
    data.setLabels(labels);
    for (BarChartDataSet ds : dataSets) {
      data.addChartDataSet(ds);
    }
    return data;
  }

  private void applyResponsiveOptions(ChartOptions options) {
    options.setResponsive(true);
    options.setMaintainAspectRatio(false);
  }

  private CartesianLinearTicks integerTicks() {
    CartesianLinearTicks ticks = new CartesianLinearTicks();
    ticks.setPrecision(0);
    return ticks;
  }

  private String truncate(String s, int maxLen) {
    if (s == null) {
      return "";
    }
    return s.length() <= maxLen ? s : s.substring(0, maxLen - 1) + "\u2026";
  }
}
