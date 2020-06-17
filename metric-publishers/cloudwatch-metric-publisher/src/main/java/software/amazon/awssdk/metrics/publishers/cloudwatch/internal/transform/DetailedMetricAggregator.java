package software.amazon.awssdk.metrics.publishers.cloudwatch.internal.transform;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.metrics.SdkMetric;
import software.amazon.awssdk.services.cloudwatch.model.Dimension;
import software.amazon.awssdk.services.cloudwatch.model.MetricDatum;
import software.amazon.awssdk.services.cloudwatch.model.StandardUnit;

/**
 * An implementation of {@link MetricAggregator} that stores all values and counts for a given metric/dimension pair
 * until they can be added to a {@link MetricDatum}.
 */
@SdkInternalApi
class DetailedMetricAggregator implements MetricAggregator {
    private final SdkMetric<?> metric;
    private final List<Dimension> dimensions;
    private final StandardUnit unit;

    private final Map<Double, DetailedMetrics> metricDetails = new HashMap<>();

    DetailedMetricAggregator(MetricAggregatorKey key, StandardUnit unit) {
        this.metric = key.metric();
        this.dimensions = key.dimensions();
        this.unit = unit;
    }

    @Override
    public SdkMetric<?> metric() {
        return metric;
    }

    @Override
    public List<Dimension> dimensions() {
        return dimensions;
    }

    @Override
    public void addMetricValue(double value) {
        metricDetails.computeIfAbsent(value, v -> new DetailedMetrics(value)).metricCount++;
    }

    @Override
    public StandardUnit unit() {
        return unit;
    }

    public Collection<DetailedMetrics> detailedMetrics() {
        return Collections.unmodifiableCollection(metricDetails.values());
    }

    public class DetailedMetrics {
        private final double metricValue;
        private int metricCount = 0;

        private DetailedMetrics(double metricValue) {
            this.metricValue = metricValue;
        }

        public double metricValue() {
            return metricValue;
        }

        public int metricCount() {
            return metricCount;
        }
    }
}
