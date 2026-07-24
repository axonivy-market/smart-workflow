package com.axonivy.utils.smart.workflow.governance.history.analytic.chart;

import java.time.LocalDate;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

public class TestAbstractChartBuilder {

  @Test
  void padTimelineToMinDays_emptyMap_paddedToMinDaysWithSupplierValues() {
    TreeMap<LocalDate, Long> map = new TreeMap<>();
    AbstractChartBuilder.padTimelineToMinDays(map, () -> 99L);
    assertThat(map).hasSize(AbstractChartBuilder.ChartConfig.MIN_TIMELINE_DAYS);
    assertThat(map.values()).allMatch(v -> v == 99L);
  }

  @Test
  void padTimelineToMinDays_gapBetweenDates_gapFilledWithSupplierValue() {
    LocalDate day1 = LocalDate.of(2025, 1, 1);
    LocalDate day3 = LocalDate.of(2025, 1, 3);
    TreeMap<LocalDate, Long> map = new TreeMap<>();
    map.put(day1, 10L);
    map.put(day3, 20L);
    AbstractChartBuilder.padTimelineToMinDays(map, () -> 99L);
    assertThat(map.get(LocalDate.of(2025, 1, 2))).isEqualTo(99L);
  }

  @Test
  void padTimelineToMinDays_existingValuesNotOverwritten() {
    LocalDate day = LocalDate.of(2025, 1, 1);
    TreeMap<LocalDate, Long> map = new TreeMap<>();
    map.put(day, 42L);
    AbstractChartBuilder.padTimelineToMinDays(map, () -> 0L);
    assertThat(map.get(day)).isEqualTo(42L);
  }

  @Test
  void padTimelineToMinDays_singleEntry_paddedForwardToMinDays() {
    LocalDate day = LocalDate.of(2025, 1, 1);
    TreeMap<LocalDate, Long> map = new TreeMap<>();
    map.put(day, 5L);
    AbstractChartBuilder.padTimelineToMinDays(map, () -> 0L);
    assertThat(map.size()).isGreaterThanOrEqualTo(AbstractChartBuilder.ChartConfig.MIN_TIMELINE_DAYS);
    assertThat(map.lastKey()).isAfter(day);
  }
}
