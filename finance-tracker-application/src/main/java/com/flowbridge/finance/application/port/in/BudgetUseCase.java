package com.flowbridge.finance.application.port.in;

import com.flowbridge.finance.domain.model.Budget;
import com.flowbridge.finance.domain.model.Category;

import java.util.List;
import java.util.UUID;

public interface BudgetUseCase {
    Budget createOrUpdateBudget(CreateBudgetCommand command);
    void deleteBudget(UUID budgetId, UUID userId);
    Budget getBudgetById(UUID budgetId, UUID userId);
    List<Budget> getBudgetsByUserId(UUID userId);
    /**
     * Checks current spending for the given category against the user's budget.
     * Returns the budget if a limit exists, throws BudgetExceededException if limit is breached.
     */
    void checkBudget(UUID userId, Category category, java.math.BigDecimal newAmount);
}
