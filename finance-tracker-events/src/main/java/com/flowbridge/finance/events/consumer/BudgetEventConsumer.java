package com.flowbridge.finance.events.consumer;

import com.flowbridge.finance.events.BudgetCreatedEvent;
import com.flowbridge.finance.events.BudgetExceededEvent;
import com.flowbridge.finance.events.FinanceTopics;
import io.micrometer.core.instrument.MeterRegistry;
import org.flowbridge.starter.annotation.FlowBridgeListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Subscribes to budget-related events via FlowBridge.
 *
 * BudgetExceeded drives alert/notification downstream:
 *  - Email notification (with retry + exponential backoff)
 *  - Push notification
 *  - Dashboard real-time alert (WebSocket)
 *
 * Failed notifications are routed to the DLQ after exhausting retries.
 */
@Component
public class BudgetEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(BudgetEventConsumer.class);

    private final MeterRegistry meterRegistry;

    public BudgetEventConsumer(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @FlowBridgeListener(topic = FinanceTopics.BUDGET_CREATED)
    public void onBudgetCreated(BudgetCreatedEvent event) {
        log.info("[FlowBridge] ▶ CONSUMED BudgetCreatedEvent: budgetId={}, userId={}, category={}, limit={}, period={}, traceId={}",
                event.getBudgetId(), event.getUserId(),
                event.getCategory(), event.getLimitAmount(), event.getPeriod(), event.getTraceId());
        meterRegistry.counter("flowbridge.events.consumed",
                "topic", FinanceTopics.BUDGET_CREATED,
                "status", "success").increment();
    }

    @FlowBridgeListener(topic = FinanceTopics.BUDGET_EXCEEDED)
    public void onBudgetExceeded(BudgetExceededEvent event) {
        log.warn("[FlowBridge] 🚨 CONSUMED BudgetExceededEvent: userId={}, category={}, spent={}, limit={}, overage={}, traceId={}",
                event.getUserId(), event.getCategory(),
                event.getSpent(), event.getLimit(), event.getOverageAmount(), event.getTraceId());

        try {
            sendNotificationWithRetry(event);
            meterRegistry.counter("flowbridge.events.consumed",
                    "topic", FinanceTopics.BUDGET_EXCEEDED,
                    "status", "success").increment();
        } catch (Exception e) {
            meterRegistry.counter("flowbridge.events.consumed",
                    "topic", FinanceTopics.BUDGET_EXCEEDED,
                    "status", "failure").increment();
            throw e;
        }
    }

    private void sendNotificationWithRetry(BudgetExceededEvent event) {
        int maxAttempts = 3;
        long backoffMs = 1000; // start with 1 second
        double multiplier = 2.0; // exponential backoff

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                log.info("[Notification Service] Attempt {} to send budget exceeded notification to user: {}", attempt, event.getUserId());

                // Simulate a temporary failure for Category.ENTERTAINMENT (fails on attempt 1, succeeds on attempt 2)
                if ("ENTERTAINMENT".equals(event.getCategory()) && attempt == 1) {
                    throw new RuntimeException("Notification network timeout (simulated once)");
                }
                // Simulate a permanent failure for Category.HOUSING (fails on all attempts, routes to DLQ)
                if ("HOUSING".equals(event.getCategory())) {
                    throw new RuntimeException("Notification server down (simulated permanent)");
                }

                log.info("[Notification Service] Notification successfully sent to user: {}", event.getUserId());
                return;
            } catch (Exception e) {
                log.error("[Notification Service] Attempt {} failed: {}", attempt, e.getMessage());
                if (attempt == maxAttempts) {
                    // Re-throw so that EventBus registers routing failure and routes to DLQ
                    throw new RuntimeException("All retry attempts failed to send notification", e);
                }
                try {
                    log.info("[Notification Service] Sleeping for {} ms before next retry attempt", backoffMs);
                    Thread.sleep(backoffMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Retry sleep interrupted", ie);
                }
                backoffMs = (long) (backoffMs * multiplier);
            }
        }
    }
}
