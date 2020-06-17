package software.amazon.awssdk.metrics.publishers.cloudwatch;

import java.time.Instant;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import software.amazon.awssdk.metrics.MetricCollection;
import software.amazon.awssdk.metrics.MetricRecord;
import software.amazon.awssdk.metrics.SdkMetric;

/**
 * An implementation of {@link MetricCollection} that sets a static time for the {@link #creationTime()}. This makes it easier
 * to test aggregation behavior, because the times can be fixed instead of regenerated each time the {@code MetricCollection} is
 * created.
 */
public class FixedTimeMetricCollection implements MetricCollection {
    private final MetricCollection delegate;
    private final Instant creationTime;

    public FixedTimeMetricCollection(MetricCollection delegate) {
        this(delegate, Instant.EPOCH);
    }

    public FixedTimeMetricCollection(MetricCollection delegate,
                                     Instant creationTime) {
        this.delegate = delegate;
        this.creationTime = creationTime;
    }

    @Override
    public String name() {
        return delegate.name();
    }

    @Override
    public <T> List<T> metricValues(SdkMetric<T> metric) {
        return delegate.metricValues(metric);
    }

    @Override
    public List<MetricCollection> children() {
        return delegate.children()
                       .stream()
                       .map(c -> new FixedTimeMetricCollection(c, creationTime))
                       .collect(Collectors.toList());
    }

    @Override
    public Instant creationTime() {
        return creationTime;
    }

    @Override
    public Iterator<MetricRecord<?>> iterator() {
        return delegate.iterator();
    }
}
