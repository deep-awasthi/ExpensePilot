package com.flowbridge.finance.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class Budget {
    private UUID id;
    private UUID userId;
    private Category category;
    private BigDecimal limitAmount;
    private BudgetPeriod period;
    private Instant createdAt;

    public Budget() {
    }

    public Budget(UUID id, UUID userId, Category category, BigDecimal limitAmount, BudgetPeriod period, Instant createdAt) {
        this.id = id;
        this.userId = userId;
        this.category = category;
        this.limitAmount = limitAmount;
        this.period = period;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    public BigDecimal getLimitAmount() { return limitAmount; }
    public void setLimitAmount(BigDecimal limitAmount) { this.limitAmount = limitAmount; }
    public BudgetPeriod getPeriod() { return period; }
    public void setPeriod(BudgetPeriod period) { this.period = period; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private UUID id;
        private UUID userId;
        private Category category;
        private BigDecimal limitAmount;
        private BudgetPeriod period;
        private Instant createdAt;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder userId(UUID userId) { this.userId = userId; return this; }
        public Builder category(Category category) { this.category = category; return this; }
        public Builder limitAmount(BigDecimal limitAmount) { this.limitAmount = limitAmount; return this; }
        public Builder period(BudgetPeriod period) { this.period = period; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public Budget build() { return new Budget(id, userId, category, limitAmount, period, createdAt); }
    }
}
