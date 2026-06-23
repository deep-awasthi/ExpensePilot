package com.flowbridge.finance.domain.exception;

public class BudgetExceededException extends RuntimeException {
    private final String category;
    private final java.math.BigDecimal spent;
    private final java.math.BigDecimal limit;

    public BudgetExceededException(String category, java.math.BigDecimal spent, java.math.BigDecimal limit) {
        super(String.format("Budget exceeded for category '%s': spent %.2f of %.2f limit", category, spent, limit));
        this.category = category;
        this.spent = spent;
        this.limit = limit;
    }

    public String getCategory() { return category; }
    public java.math.BigDecimal getSpent() { return spent; }
    public java.math.BigDecimal getLimit() { return limit; }
}
