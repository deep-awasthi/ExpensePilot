package com.flowbridge.finance.application.port.in;

import com.flowbridge.finance.domain.model.BudgetPeriod;
import com.flowbridge.finance.domain.model.Category;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public class CreateBudgetCommand {

    private UUID userId;

    @NotNull(message = "Category is required")
    private Category category;

    @NotNull(message = "Limit amount is required")
    @DecimalMin(value = "0.01", message = "Limit amount must be greater than 0")
    private BigDecimal limitAmount;

    @NotNull(message = "Budget period is required")
    private BudgetPeriod period;

    public CreateBudgetCommand() {}

    public CreateBudgetCommand(UUID userId, Category category, BigDecimal limitAmount, BudgetPeriod period) {
        this.userId = userId;
        this.category = category;
        this.limitAmount = limitAmount;
        this.period = period;
    }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    public BigDecimal getLimitAmount() { return limitAmount; }
    public void setLimitAmount(BigDecimal limitAmount) { this.limitAmount = limitAmount; }
    public BudgetPeriod getPeriod() { return period; }
    public void setPeriod(BudgetPeriod period) { this.period = period; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private UUID userId;
        private Category category;
        private BigDecimal limitAmount;
        private BudgetPeriod period;

        public Builder userId(UUID userId) { this.userId = userId; return this; }
        public Builder category(Category category) { this.category = category; return this; }
        public Builder limitAmount(BigDecimal limitAmount) { this.limitAmount = limitAmount; return this; }
        public Builder period(BudgetPeriod period) { this.period = period; return this; }
        public CreateBudgetCommand build() { return new CreateBudgetCommand(userId, category, limitAmount, period); }
    }
}
