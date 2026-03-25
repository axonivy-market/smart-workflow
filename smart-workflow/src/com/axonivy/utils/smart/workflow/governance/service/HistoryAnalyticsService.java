package com.axonivy.utils.smart.workflow.governance.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.primefaces.model.charts.ChartData;
import org.primefaces.model.charts.ChartOptions;
import org.primefaces.model.charts.bar.BarChartDataSet;
import org.primefaces.model.charts.bar.BarChartModel;
import org.primefaces.model.charts.bar.BarChartOptions;
import org.primefaces.model.charts.axes.cartesian.CartesianScales;
import org.primefaces.model.charts.axes.cartesian.linear.CartesianLinearAxes;
import org.primefaces.model.charts.axes.cartesian.linear.CartesianLinearTicks;
import org.primefaces.model.charts.donut.DonutChartModel;
import org.primefaces.model.charts.donut.DonutChartOptions;
import org.primefaces.model.charts.line.LineChartDataSet;
import org.primefaces.model.charts.line.LineChartModel;
import org.primefaces.model.charts.line.LineChartOptions;
import org.primefaces.model.charts.optionconfig.legend.Legend;
import org.primefaces.model.charts.pie.PieChartDataSet;

import com.axonivy.utils.smart.workflow.governance.history.entity.AgentConversationEntry;
import com.axonivy.utils.smart.workflow.governance.ui.model.DashboardKpi;
import com.axonivy.utils.smart.workflow.governance.utils.ChatHistoryJsonParser;

public class HistoryAnalyticsService {

  private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("MMM dd");
  private static final List<String> CHART_COLORS =
      Arrays.asList("#0060c8", "#17a2b8", "#28a745", "#fd7e14", "#6f42c1");

  // ── KPI ──────────────────────────────────────────────────────────────────

  public DashboardKpi computeKpi(List<AgentConversationEntry> current, List<AgentConversationEntry> previous) {
    if (current.isEmpty()) {
      return DashboardKpi.empty();
    }
    int sessions = current.size();
    long tokens = current.stream().mapToLong(ChatHistoryJsonParser::getTotalTokens).sum();
    long avgMs = (long) current.stream()
        .mapToLong(ChatHistoryJsonParser::getAvgDurationMs)
        .average().orElse(0);

    Map<String, Long> modelCounts = current.stream()
        .collect(Collectors.groupingBy(ChatHistoryJsonParser::getModelName, Collectors.counting()));
    Map.Entry<String, Long> top = modelCounts.entrySet().stream()
        .max(Map.Entry.comparingByValue())
        .orElse(Map.entry("\u2014", 0L));
    int topPct = sessions == 0 ? 0 : (int) Math.round(100.0 * top.getValue() / sessions);

    long prevTokens = previous.stream().mapToLong(ChatHistoryJsonParser::getTotalTokens).sum();
    long prevMs = (long) previous.stream()
        .mapToLong(ChatHistoryJsonParser::getAvgDurationMs)
        .average().orElse(0);

    return new DashboardKpi(sessions, tokens, avgMs, top.getKey(), topPct,
        trendPct(previous.size(), sessions),
        trendPct(prevTokens, tokens),
        trendPct(prevMs, avgMs));
  }

  private int trendPct(long oldVal, long newVal) {
    if (oldVal == 0) {
      return 0;
    }
    return (int) Math.round(100.0 * (newVal - oldVal) / oldVal);
  }

  // ── Chart A: Token Usage Over Time (line) ────────────────────────────────

  public LineChartModel buildTokenTimeline(List<AgentConversationEntry> entries) {
    Map<String, Long> tokensByDay = new LinkedHashMap<>();
    entries.stream()
        .filter(e -> e.getLastUpdated() != null)
        .sorted((a, b) -> a.getLastUpdated().compareTo(b.getLastUpdated()))
        .forEach(e -> {
          try {
            String label = LocalDateTime.parse(e.getLastUpdated()).toLocalDate().format(DAY_FMT);
            tokensByDay.merge(label, (long) ChatHistoryJsonParser.getTotalTokens(e), Long::sum);
          } catch (Exception ignored) {}
        });

    LineChartDataSet dataSet = new LineChartDataSet();
    dataSet.setLabel("Total Tokens");
    dataSet.setBorderColor("#0060c8");
    dataSet.setBackgroundColor("rgba(0,96,200,0.15)");
    dataSet.setFill(true);
    dataSet.setTension(0.3);
    dataSet.setData(new ArrayList<>(tokensByDay.values()));

    ChartData data = new ChartData();
    data.setLabels(new ArrayList<>(tokensByDay.keySet()));
    data.addChartDataSet(dataSet);

    LineChartModel model = new LineChartModel();
    model.setData(data);

    CartesianLinearAxes yAxis = new CartesianLinearAxes();
    yAxis.setTicks(integerTicks());

    CartesianScales scales = new CartesianScales();
    scales.addYAxesData(yAxis);

    LineChartOptions options = new LineChartOptions();
    applyResponsiveOptions(options);
    options.setScales(scales);
    Legend legend = new Legend();
    legend.setDisplay(false);
    options.setLegend(legend);
    model.setOptions(options);

    return model;
  }

  // ── Chart B: Model Distribution (donut) ──────────────────────────────────

  public DonutChartModel buildModelDistribution(List<AgentConversationEntry> entries) {
    Map<String, Long> counts = entries.stream()
        .collect(Collectors.groupingBy(ChatHistoryJsonParser::getModelName, Collectors.counting()));

    List<String> labels = new ArrayList<>(counts.keySet());
    List<Number> values = new ArrayList<>(counts.values());
    List<String> colors = new ArrayList<>();
    for (int i = 0; i < labels.size(); i++) {
      colors.add(CHART_COLORS.get(i % CHART_COLORS.size()));
    }

    PieChartDataSet dataSet = new PieChartDataSet();
    dataSet.setData(values);
    dataSet.setBackgroundColor(colors);

    ChartData data = new ChartData();
    data.setLabels(labels);
    data.addChartDataSet(dataSet);

    DonutChartModel model = new DonutChartModel();
    model.setData(data);
    DonutChartOptions donutOptions = new DonutChartOptions();
    applyResponsiveOptions(donutOptions);
    model.setOptions(donutOptions);
    return model;
  }

  // ── Chart C: Input vs Output per Day (stacked bar) ───────────────────────

  public BarChartModel buildTokenStacked(List<AgentConversationEntry> entries) {
    Map<String, Long[]> byDay = new LinkedHashMap<>();
    entries.stream()
        .filter(e -> e.getLastUpdated() != null)
        .sorted((a, b) -> a.getLastUpdated().compareTo(b.getLastUpdated()))
        .forEach(e -> {
          try {
            String label = LocalDateTime.parse(e.getLastUpdated()).toLocalDate().format(DAY_FMT);
            byDay.merge(label,
                new Long[]{ChatHistoryJsonParser.getInputTokens(e),
                           ChatHistoryJsonParser.getOutputTokens(e)},
                (existing, incoming) -> new Long[]{
                    existing[0] + incoming[0], existing[1] + incoming[1]});
          } catch (Exception ignored) {}
        });

    List<String> labels = new ArrayList<>(byDay.keySet());
    List<Number> inputData = new ArrayList<>();
    List<Number> outputData = new ArrayList<>();
    for (Long[] vals : byDay.values()) {
      inputData.add(vals[0]);
      outputData.add(vals[1]);
    }

    BarChartDataSet inputDataSet = new BarChartDataSet();
    inputDataSet.setLabel("Input Tokens");
    inputDataSet.setBackgroundColor("#0060c8");
    inputDataSet.setData(inputData);
    inputDataSet.setStack("tokens");

    BarChartDataSet outputDataSet = new BarChartDataSet();
    outputDataSet.setLabel("Output Tokens");
    outputDataSet.setBackgroundColor("#17a2b8");
    outputDataSet.setData(outputData);
    outputDataSet.setStack("tokens");

    ChartData data = new ChartData();
    data.setLabels(labels);
    data.addChartDataSet(inputDataSet);
    data.addChartDataSet(outputDataSet);

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
    model.setData(data);
    model.setOptions(options);
    return model;
  }

  // ── Chart D: Top 5 Processes by Token Usage (horizontal bar) ────────────

  public BarChartModel buildTopCases(List<AgentConversationEntry> entries) {
    Map<String, Long> byCase = entries.stream()
        .collect(Collectors.groupingBy(
            e -> e.getProcessName() != null && !e.getProcessName().isEmpty() ? e.getProcessName() : "unknown",
            Collectors.summingLong(ChatHistoryJsonParser::getTotalTokens)));

    List<Map.Entry<String, Long>> sorted = byCase.entrySet().stream()
        .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
        .limit(5)
        .collect(Collectors.toList());

    List<String> labels = sorted.stream()
        .map(e -> truncate(e.getKey(), 20))
        .collect(Collectors.toList());
    List<Number> values = sorted.stream()
        .map(Map.Entry::getValue)
        .collect(Collectors.toList());

    BarChartDataSet dataSet = new BarChartDataSet();
    dataSet.setLabel("Total Tokens");
    dataSet.setBackgroundColor("#0060c8");
    dataSet.setData(values);

    ChartData data = new ChartData();
    data.setLabels(labels);
    data.addChartDataSet(dataSet);

    CartesianLinearAxes xAxis = new CartesianLinearAxes();
    xAxis.setTicks(integerTicks());

    CartesianScales scales = new CartesianScales();
    scales.addXAxesData(xAxis);

    BarChartOptions options = new BarChartOptions();
    applyResponsiveOptions(options);
    options.setIndexAxis("y");
    options.setScales(scales);

    BarChartModel model = new BarChartModel();
    model.setData(data);
    model.setOptions(options);
    return model;
  }

  // ── Chart E: Response Time Distribution (horizontal histogram) ───────────

  public BarChartModel buildResponseTimeHistogram(List<AgentConversationEntry> entries) {
    long[] buckets = new long[4];
    for (AgentConversationEntry entry : entries) {
      long ms = ChatHistoryJsonParser.getAvgDurationMs(entry);
      if (ms < 5000) {
        buckets[0]++;
      } else if (ms < 10000) {
        buckets[1]++;
      } else if (ms < 15000) {
        buckets[2]++;
      } else {
        buckets[3]++;
      }
    }

    List<String> labels = Arrays.asList("< 5 s", "5–10 s", "10–15 s", "> 15 s");
    List<String> colors = Arrays.asList("#28a745", "#0060c8", "#fd7e14", "#dc3545");
    List<Number> values = new ArrayList<>();
    for (long v : buckets) {
      values.add(v);
    }

    BarChartDataSet dataSet = new BarChartDataSet();
    dataSet.setLabel("Sessions");
    dataSet.setBackgroundColor(colors);
    dataSet.setData(values);

    ChartData data = new ChartData();
    data.setLabels(labels);
    data.addChartDataSet(dataSet);

    CartesianLinearAxes xAxis2 = new CartesianLinearAxes();
    xAxis2.setTicks(integerTicks());

    CartesianScales scales2 = new CartesianScales();
    scales2.addXAxesData(xAxis2);

    BarChartOptions options = new BarChartOptions();
    applyResponsiveOptions(options);
    options.setIndexAxis("y");
    options.setScales(scales2);

    BarChartModel model = new BarChartModel();
    model.setData(data);
    model.setOptions(options);
    return model;
  }

  // ── Helpers ───────────────────────────────────────────────────────────────

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
