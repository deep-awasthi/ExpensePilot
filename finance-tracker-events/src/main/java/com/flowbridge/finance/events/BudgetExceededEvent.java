package com.flowbridge.finance.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Published when a user's spending on a category exceeds the configured budget limit.
 * Downstream consumers (notification service, analytics) subscribe to this topic.
 */
public class BudgetExceededEvent {

    private UUID userId;
    private String category;
    private BigDecimal spent;
    private BigDecimal limit;
    private BigDecimal overageAmount;  // spent - limit
    private Instant occurredAt;
    private String traceId;

    public BudgetExceededEvent() {}

    public BudgetExceededEvent(UUID userId, String category,
                                BigDecimal spent, BigDecimal limit, Instant occurredAt, String traceId) {
        this.userId = userId;
        this.category = category;
        this.spent = spent;
        this.limit = limit;
        this.overageAmount = spent.subtract(limit);
        this.occurredAt = occurredAt;
        this.traceId = traceId;
    }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public BigDecimal getSpent() { return spent; }
    public void setSpent(BigDecimal spent) { this.spent = spent; }
    public BigDecimal getLimit() { return limit; }
    public void setLimit(BigDecimal limit) { this.limit = limit; }
    public BigDecimal getOverageAmount() { return overageAmount; }
    public void setOverageAmount(BigDecimal overageAmount) { this.overageAmount = overageAmount; }
    public Instant getOccurredAt() { return occurredAt; }
    public void setOccurredAt(Instant occurredAt) { this.occurredAt = occurredAt; }
    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }

    @Override
    public String toString() {
        return "BudgetExceededEvent{userId=" + userId + ", category=" + category +
                ", spent=" + spent + ", limit=" + limit + ", overage=" + overageAmount + ", traceId=" + traceId + "}";
    }
}
