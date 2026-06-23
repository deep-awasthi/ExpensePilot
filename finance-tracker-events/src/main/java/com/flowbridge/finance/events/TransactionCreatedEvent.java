package com.flowbridge.finance.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Published when a transaction is successfully created.
 */
public class TransactionCreatedEvent {

    private UUID transactionId;
    private UUID userId;
    private String type;       // INCOME or EXPENSE
    private String category;
    private BigDecimal amount;
    private String description;
    private Instant occurredAt;
    private String traceId;

    public TransactionCreatedEvent() {}

    public TransactionCreatedEvent(UUID transactionId, UUID userId, String type,
                                   String category, BigDecimal amount,
                                   String description, Instant occurredAt, String traceId) {
        this.transactionId = transactionId;
        this.userId = userId;
        this.type = type;
        this.category = category;
        this.amount = amount;
        this.description = description;
        this.occurredAt = occurredAt;
        this.traceId = traceId;
    }

    public UUID getTransactionId() { return transactionId; }
    public void setTransactionId(UUID transactionId) { this.transactionId = transactionId; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Instant getOccurredAt() { return occurredAt; }
    public void setOccurredAt(Instant occurredAt) { this.occurredAt = occurredAt; }
    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }

    @Override
    public String toString() {
        return "TransactionCreatedEvent{transactionId=" + transactionId +
                ", userId=" + userId + ", type=" + type + ", category=" + category +
                ", amount=" + amount + ", traceId=" + traceId + "}";
    }
}
