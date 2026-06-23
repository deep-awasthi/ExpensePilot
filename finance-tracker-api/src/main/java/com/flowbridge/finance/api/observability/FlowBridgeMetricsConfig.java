package com.flowbridge.finance.api.observability;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Gauge;
import org.flowbridge.core.application.port.out.DeadLetterStore;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * Exposes custom Micrometer observability metrics for the FlowBridge event layer,
 * including a real-time Gauge showing the size of the Dead Letter Queue.
 */
@Configuration
public class FlowBridgeMetricsConfig {

    private final MeterRegistry meterRegistry;
    private final DeadLetterStore deadLetterStore;

    public FlowBridgeMetricsConfig(MeterRegistry meterRegistry, DeadLetterStore deadLetterStore) {
        this.meterRegistry = meterRegistry;
        this.deadLetterStore = deadLetterStore;
    }

    @PostConstruct
    public void registerMetrics() {
        // Register DLQ Size Gauge
        Gauge.builder("flowbridge.dlq.size", deadLetterStore, store -> {
            try {
                return store.findAll().size();
            } catch (Exception e) {
                return 0;
            }
        })
        .description("Real-time size of the FlowBridge Dead Letter Queue")
        .register(meterRegistry);
    }
}
