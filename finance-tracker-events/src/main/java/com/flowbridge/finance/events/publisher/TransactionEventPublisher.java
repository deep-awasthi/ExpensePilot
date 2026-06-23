package com.flowbridge.finance.events.publisher;

import com.flowbridge.finance.application.port.out.TransactionEventPort;
import com.flowbridge.finance.events.FinanceTopics;
import com.flowbridge.finance.events.TransactionCreatedEvent;
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
 * Publishes transaction-related domain events through FlowBridge.
 * Depends only on the EventBus abstraction — the backing provider
 * (LOCAL / EMBEDDED / KAFKA) is resolved at runtime via configuration.
 */
@Component
public class TransactionEventPublisher implements TransactionEventPort {

    private static final Logger log = LoggerFactory.getLogger(TransactionEventPublisher.class);

    private final EventBus eventBus;
    private final MeterRegistry meterRegistry;

    public TransactionEventPublisher(EventBus eventBus, MeterRegistry meterRegistry) {
        this.eventBus = eventBus;
        this.meterRegistry = meterRegistry;
    }

    public void publishTransactionCreated(UUID transactionId, UUID userId,
                                           String type, String category,
                                           BigDecimal amount, String description) {
        String traceId = UUID.randomUUID().toString();
        TransactionCreatedEvent event = new TransactionCreatedEvent(
                transactionId, userId, type, category, amount, description, Instant.now(), traceId);

        Map<String, String> headers = Map.of(
                "source",      "finance-tracker",
                "event-type",  "TransactionCreated",
                "user-id",     userId.toString(),
                "trace-id",    traceId
        );

        log.info("[FlowBridge] Publishing TransactionCreatedEvent: transactionId={}, userId={}, type={}, category={}, amount={}",
                transactionId, userId, type, category, amount);

        try {
            eventBus.publish(FinanceTopics.TRANSACTION_CREATED, event, headers);
            meterRegistry.counter("flowbridge.events.published",
                    "topic", FinanceTopics.TRANSACTION_CREATED,
                    "status", "success").increment();
        } catch (Exception e) {
            meterRegistry.counter("flowbridge.events.published",
                    "topic", FinanceTopics.TRANSACTION_CREATED,
                    "status", "failure").increment();
            throw e;
        }
    }
}
