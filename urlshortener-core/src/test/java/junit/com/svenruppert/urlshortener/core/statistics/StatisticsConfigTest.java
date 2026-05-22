package junit.com.svenruppert.urlshortener.core.statistics;

import com.svenruppert.urlshortener.core.statistics.StatisticsConfig;
import org.junit.jupiter.api.Test;

import static com.svenruppert.urlshortener.core.statistics.StatisticsConfig.DEFAULT_AGGREGATOR_INTERVAL_SECONDS;
import static com.svenruppert.urlshortener.core.statistics.StatisticsConfig.DEFAULT_HOT_WINDOW_DAYS;
import static com.svenruppert.urlshortener.core.statistics.StatisticsConfig.DEFAULT_WRITER_BATCH_SIZE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatisticsConfigTest {

  @Test
  void defaultConstructor_usesDocumentedDefaults() {
    StatisticsConfig c = new StatisticsConfig();
    assertEquals(DEFAULT_HOT_WINDOW_DAYS, c.hotWindowDays());
    assertEquals(DEFAULT_WRITER_BATCH_SIZE, c.writerBatchSize());
    assertEquals(DEFAULT_AGGREGATOR_INTERVAL_SECONDS, c.aggregatorIntervalSeconds());
    assertTrue(c.isStatisticsEnabled());
  }

  @Test
  void setHotWindowDays_acceptsExactlyOne() {
    StatisticsConfig c = new StatisticsConfig();
    c.setHotWindowDays(1);
    assertEquals(1, c.hotWindowDays());
  }

  @Test
  void setHotWindowDays_rejectsZeroAndNegative() {
    StatisticsConfig c = new StatisticsConfig();
    assertThrows(IllegalArgumentException.class, () -> c.setHotWindowDays(0));
    assertThrows(IllegalArgumentException.class, () -> c.setHotWindowDays(-1));
  }

  @Test
  void setWriterBatchSize_acceptsExactlyOne() {
    StatisticsConfig c = new StatisticsConfig();
    c.setWriterBatchSize(1);
    assertEquals(1, c.writerBatchSize());
  }

  @Test
  void setWriterBatchSize_rejectsZeroAndNegative() {
    StatisticsConfig c = new StatisticsConfig();
    assertThrows(IllegalArgumentException.class, () -> c.setWriterBatchSize(0));
    assertThrows(IllegalArgumentException.class, () -> c.setWriterBatchSize(-3));
  }

  @Test
  void setAggregatorIntervalSeconds_acceptsExactlyMinimum() {
    StatisticsConfig c = new StatisticsConfig();
    c.setAggregatorIntervalSeconds(60);
    assertEquals(60, c.aggregatorIntervalSeconds());
  }

  @Test
  void setAggregatorIntervalSeconds_rejectsBelowMinimum() {
    StatisticsConfig c = new StatisticsConfig();
    assertThrows(IllegalArgumentException.class, () -> c.setAggregatorIntervalSeconds(59));
    assertThrows(IllegalArgumentException.class, () -> c.setAggregatorIntervalSeconds(0));
    assertThrows(IllegalArgumentException.class, () -> c.setAggregatorIntervalSeconds(-100));
  }

  @Test
  void setStatisticsEnabled_togglesFlag() {
    StatisticsConfig c = new StatisticsConfig();
    c.setStatisticsEnabled(false);
    assertFalse(c.isStatisticsEnabled());
    c.setStatisticsEnabled(true);
    assertTrue(c.isStatisticsEnabled());
  }

  @Test
  void toString_containsAllFields() {
    StatisticsConfig c = new StatisticsConfig();
    c.setHotWindowDays(3);
    c.setWriterBatchSize(50);
    c.setAggregatorIntervalSeconds(120);
    c.setStatisticsEnabled(false);

    String s = c.toString();
    assertTrue(s.contains("hotWindowDays=3"));
    assertTrue(s.contains("writerBatchSize=50"));
    assertTrue(s.contains("aggregatorIntervalSeconds=120"));
    assertTrue(s.contains("statisticsEnabled=false"));
  }
}
