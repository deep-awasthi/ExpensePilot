package com.flowbridge.finance.events.publisher;

import com.flowbridge.finance.application.port.out.BudgetEventPort;
import com.flowbridge.finance.events.BudgetCreatedEvent;
import com.flowbridge.finance.events.BudgetExceededEvent;
import com.flowbridge.finance.events.FinanceTopics;
import io.micrometer.core.instrument.MeterRegistry;
import org.flowbridge.core.application.port.in.EventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Publishes budget-related domain events through FlowBridge.
 * Depends only on the EventBus abstraction — the backing provider
 * (LOCAL / EMBEDDED / KAFKA) is resolved at runtime via configuration.
 */
@Component
public class BudgetEventPublisher implements BudgetEventPort {

    private static final Logger log = LoggerFactory.getLogger(BudgetEventPublisher.class);

    private final EventBus eventBus;
    private final MeterRegistry meterRegistry;

    public BudgetEventPublisher(EventBus eventBus, MeterRegistry meterRegistry) {
        this.eventBus = eventBus;
        this.meterRegistry = meterRegistry;
    }

    public void publishBudgetCreated(UUID budgetId, UUID userId,
                                      String category, BigDecimal limitAmount, String period) {
        String traceId = UUID.randomUUID().toString();
        BudgetCreatedEvent event = new BudgetCreatedEvent(
                budgetId, userId, category, limitAmount, period, Instant.now(), traceId);

        Map<String, String> headers = Map.of(
                "source",     "finance-tracker",
                "event-type", "BudgetCreated",
                "user-id",    userId.toString(),
                "trace-id",   traceId
        );

        log.info("[FlowBridge] Publishing BudgetCreatedEvent: budgetId={}, userId={}, category={}, limit={}",
                budgetId, userId, category, limitAmount);

        try {
            eventBus.publish(FinanceTopics.BUDGET_CREATED, event, headers);
            meterRegistry.counter("flowbridge.events.published",
                    "topic", FinanceTopics.BUDGET_CREATED,
                    "status", "success").increment();
        } catch (Exception e) {
            meterRegistry.counter("flowbridge.events.published",
                    "topic", FinanceTopics.BUDGET_CREATED,
                    "status", "failure").increment();
            throw e;
        }
    }

    public void publishBudgetExceeded(UUID userId, String category,
                                       BigDecimal spent, BigDecimal limit) {
        String traceId = UUID.randomUUID().toString();
        BudgetExceededEvent event = new BudgetExceededEvent(
                userId, category, spent, limit, Instant.now(), traceId);

        Map<String, String> headers = Map.of(
                "source",     "finance-tracker",
                "event-type", "BudgetExceeded",
                "user-id",    userId.toString(),
                "trace-id",   traceId
        );

        log.warn("[FlowBridge] Publishing BudgetExceededEvent: userId={}, category={}, spent={}, limit={}, overage={}",
                userId, category, spent, limit, event.getOverageAmount());

        try {
            eventBus.publish(FinanceTopics.BUDGET_EXCEEDED, event, headers);
            meterRegistry.counter("flowbridge.events.published",
                    "topic", FinanceTopics.BUDGET_EXCEEDED,
                    "status", "success").increment();
        } catch (Exception e) {
            meterRegistry.counter("flowbridge.events.published",
                    "topic", FinanceTopics.BUDGET_EXCEEDED,
                    "status", "failure").increment();
            throw e;
        }
    }
}
