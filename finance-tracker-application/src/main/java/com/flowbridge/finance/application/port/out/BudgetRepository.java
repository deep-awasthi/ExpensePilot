package com.flowbridge.finance.application.port.out;

import com.flowbridge.finance.domain.model.Budget;
import com.flowbridge.finance.domain.model.Category;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BudgetRepository {
    Budget save(Budget budget);
    Optional<Budget> findById(UUID id);
    Optional<Budget> findByUserIdAndCategory(UUID userId, Category category);
    List<Budget> findByUserId(UUID userId);
    void deleteById(UUID id);
}
