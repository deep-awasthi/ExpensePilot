package com.flowbridge.finance.application.port.out;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Output port for publishing budget-related events.
 * Implemented by the infrastructure events module.
 */
public interface BudgetEventPort {
    
    void publishBudgetCreated(UUID budgetId, UUID userId, String category, BigDecimal limitAmount, String period);
    
    void publishBudgetExceeded(UUID userId, String category, BigDecimal spent, BigDecimal limit);
}
