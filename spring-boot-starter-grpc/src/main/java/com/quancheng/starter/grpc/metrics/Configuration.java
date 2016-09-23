package com.quancheng.starter.grpc.metrics;

import io.prometheus.client.CollectorRegistry;

public class Configuration {

    private static double[]         DEFAULT_LATENCY_BUCKETS = new double[] { .001, .005, .01, .05, 0.075, .1, .25, .5,
                                                                             1, 2, 5, 10 };

    private final boolean           isIncludeLatencyHistograms;
    private final CollectorRegistry collectorRegistry;
    private final double[]          latencyBuckets;

    public static Configuration cheapMetricsOnly() {
        return new Configuration(false /* isIncludeLatencyHistograms */, CollectorRegistry.defaultRegistry,
                                 DEFAULT_LATENCY_BUCKETS);
    }

    public static Configuration allMetrics() {
        return new Configuration(true /* isIncludeLatencyHistograms */, CollectorRegistry.defaultRegistry,
                                 DEFAULT_LATENCY_BUCKETS);
    }

    public Configuration withCollectorRegistry(CollectorRegistry collectorRegistry) {
        return new Configuration(isIncludeLatencyHistograms, collectorRegistry, latencyBuckets);
    }

    public Configuration withLatencyBuckets(double[] buckets) {
        return new Configuration(isIncludeLatencyHistograms, collectorRegistry, buckets);
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

    private Configuration(boolean isIncludeLatencyHistograms, CollectorRegistry collectorRegistry,
                          double[] latencyBuckets){
        this.isIncludeLatencyHistograms = isIncludeLatencyHistograms;
        this.collectorRegistry = collectorRegistry;
        this.latencyBuckets = latencyBuckets;
    }
}
