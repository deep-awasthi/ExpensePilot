package com.flowbridge.finance.events.consumer;

import com.flowbridge.finance.events.FinanceTopics;
import com.flowbridge.finance.events.TransactionCreatedEvent;
import org.flowbridge.starter.annotation.FlowBridgeListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Subscribes to finance.transaction.created events via FlowBridge.
 *
 * In production this could drive:
 *  - Analytics aggregation
 *  - Notifications
 *  - Audit logging
 *  - ML spending pattern detection
 *
 * FlowBridge's BeanPostProcessor automatically registers the @FlowBridgeListener
 * method with the EventBus at application startup.
 */
@Component
public class TransactionEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(TransactionEventConsumer.class);

    @FlowBridgeListener(topic = FinanceTopics.TRANSACTION_CREATED)
    public void onTransactionCreated(TransactionCreatedEvent event) {
        log.info("[FlowBridge] ▶ CONSUMED TransactionCreatedEvent: transactionId={}, userId={}, type={}, category={}, amount={}",
                event.getTransactionId(), event.getUserId(),
                event.getType(), event.getCategory(), event.getAmount());

        // TODO Phase 7: Forward to analytics aggregation service
        // TODO Phase 7: Trigger spending trend recalculation
    }
}
