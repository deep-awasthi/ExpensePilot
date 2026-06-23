package com.flowbridge.finance.application.port.out;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Output port for publishing transaction-related events.
 * Implemented by the infrastructure events module.
 */
public interface TransactionEventPort {

    void publishTransactionCreated(UUID transactionId, UUID userId,
                                   String type, String category,
                                   BigDecimal amount, String description);
}
