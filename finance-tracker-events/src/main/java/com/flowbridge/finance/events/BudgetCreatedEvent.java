package com.flowbridge.finance.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Published when a budget is created or updated via upsert.
 */
public class BudgetCreatedEvent {

    private UUID budgetId;
    private UUID userId;
    private String category;
    private BigDecimal limitAmount;
    private String period;     // MONTHLY or YEARLY
    private Instant occurredAt;
    private String traceId;

    public BudgetCreatedEvent() {}

    public BudgetCreatedEvent(UUID budgetId, UUID userId, String category,
                               BigDecimal limitAmount, String period, Instant occurredAt, String traceId) {
        this.budgetId = budgetId;
        this.userId = userId;
        this.category = category;
        this.limitAmount = limitAmount;
        this.period = period;
        this.occurredAt = occurredAt;
        this.traceId = traceId;
    }

    public UUID getBudgetId() { return budgetId; }
    public void setBudgetId(UUID budgetId) { this.budgetId = budgetId; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public BigDecimal getLimitAmount() { return limitAmount; }
    public void setLimitAmount(BigDecimal limitAmount) { this.limitAmount = limitAmount; }
    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }
    public Instant getOccurredAt() { return occurredAt; }
    public void setOccurredAt(Instant occurredAt) { this.occurredAt = occurredAt; }
    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }

    @Override
    public String toString() {
        return "BudgetCreatedEvent{budgetId=" + budgetId + ", userId=" + userId +
                ", category=" + category + ", limit=" + limitAmount + ", period=" + period + ", traceId=" + traceId + "}";
    }
}
