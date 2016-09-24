package com.quancheng.starter.grpc.metrics;

import io.prometheus.client.CollectorRegistry;

public class MetricsConfiguration {

    private static double[]         DEFAULT_LATENCY_BUCKETS = new double[] { .001, .005, .01, .05, 0.075, .1, .25, .5,
                                                                             1, 2, 5, 10 };

    private final boolean           isIncludeLatencyHistograms;
    private final CollectorRegistry collectorRegistry;
    private final double[]          latencyBuckets;

    public static MetricsConfiguration cheapMetricsOnly() {
        return new MetricsConfiguration(false /* isIncludeLatencyHistograms */, CollectorRegistry.defaultRegistry,
                                 DEFAULT_LATENCY_BUCKETS);
    }

    public static MetricsConfiguration allMetrics() {
        return new MetricsConfiguration(true /* isIncludeLatencyHistograms */, CollectorRegistry.defaultRegistry,
                                 DEFAULT_LATENCY_BUCKETS);
    }

    public MetricsConfiguration withCollectorRegistry(CollectorRegistry collectorRegistry) {
        return new MetricsConfiguration(isIncludeLatencyHistograms, collectorRegistry, latencyBuckets);
    }

    public MetricsConfiguration withLatencyBuckets(double[] buckets) {
        return new MetricsConfiguration(isIncludeLatencyHistograms, collectorRegistry, buckets);
    }

    public boolean isIncludeLatencyHistograms() {
        return isIncludeLatencyHistograms;
    }

    /** Returns the {@link CollectorRegistry} used to record stats. */
    public CollectorRegistry getCollectorRegistry() {
        return collectorRegistry;
    }

    /** Returns the histogram buckets to use for latency metrics. */
    public double[] getLatencyBuckets() {
        return latencyBuckets;
    }

    private MetricsConfiguration(boolean isIncludeLatencyHistograms, CollectorRegistry collectorRegistry,
                          double[] latencyBuckets){
        this.isIncludeLatencyHistograms = isIncludeLatencyHistograms;
        this.collectorRegistry = collectorRegistry;
        this.latencyBuckets = latencyBuckets;
    }
}
