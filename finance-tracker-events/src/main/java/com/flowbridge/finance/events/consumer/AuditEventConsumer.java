package com.flowbridge.finance.events.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowbridge.finance.application.port.out.AuditEventRepository;
import com.flowbridge.finance.domain.model.AuditEvent;
import com.flowbridge.finance.events.BudgetCreatedEvent;
import com.flowbridge.finance.events.BudgetExceededEvent;
import com.flowbridge.finance.events.FinanceTopics;
import com.flowbridge.finance.events.TransactionCreatedEvent;
import org.flowbridge.starter.annotation.FlowBridgeListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * Audit listener that subscribes to all key business events on the EventBus
 * and persists them as database audit logs for compliance, analytics, and history.
 */
@Component
public class AuditEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(AuditEventConsumer.class);

    private final AuditEventRepository auditEventRepository;
    private final ObjectMapper objectMapper;

    public AuditEventConsumer(AuditEventRepository auditEventRepository, ObjectMapper objectMapper) {
        this.auditEventRepository = auditEventRepository;
        this.objectMapper = objectMapper;
    }

    @FlowBridgeListener(topic = FinanceTopics.TRANSACTION_CREATED)
    public void onTransactionCreated(TransactionCreatedEvent event) {
        log.info("[Audit] Processing TransactionCreated: {}", event.getTransactionId());
        saveAuditEvent("TransactionCreated", event.getTransactionId().toString(), event);
    }

    @FlowBridgeListener(topic = FinanceTopics.BUDGET_CREATED)
    public void onBudgetCreated(BudgetCreatedEvent event) {
        log.info("[Audit] Processing BudgetCreated: {}", event.getBudgetId());
        saveAuditEvent("BudgetCreated", event.getBudgetId().toString(), event);
    }

    @FlowBridgeListener(topic = FinanceTopics.BUDGET_EXCEEDED)
    public void onBudgetExceeded(BudgetExceededEvent event) {
        log.warn("[Audit] Processing BudgetExceeded for user: {}", event.getUserId());
        saveAuditEvent("BudgetExceeded", event.getUserId().toString(), event);
    }

    private void saveAuditEvent(String eventType, String aggregateId, Object payload) {
        try {
            String jsonPayload = objectMapper.writeValueAsString(payload);
            AuditEvent auditEvent = AuditEvent.builder()
                    .id(UUID.randomUUID())
                    .eventType(eventType)
                    .aggregateId(aggregateId)
                    .payload(jsonPayload)
                    .timestamp(Instant.now())
                    .build();
            auditEventRepository.save(auditEvent);
            log.info("[Audit] Persisted audit event successfully for type: {}", eventType);
        } catch (Exception e) {
            log.error("[Audit] Failed to persist audit event", e);
        }
    }
}
