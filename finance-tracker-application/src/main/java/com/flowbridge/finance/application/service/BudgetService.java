package com.flowbridge.finance.application.service;

import com.flowbridge.finance.application.port.in.BudgetUseCase;
import com.flowbridge.finance.application.port.in.CreateBudgetCommand;
import com.flowbridge.finance.application.port.out.BudgetEventPort;
import com.flowbridge.finance.application.port.out.BudgetRepository;
import com.flowbridge.finance.application.port.out.TransactionRepository;
import com.flowbridge.finance.domain.exception.AuthenticationException;
import com.flowbridge.finance.domain.exception.BudgetExceededException;
import com.flowbridge.finance.domain.exception.ResourceNotFoundException;
import com.flowbridge.finance.domain.model.Budget;
import com.flowbridge.finance.domain.model.Category;
import com.flowbridge.finance.domain.model.Transaction;
import com.flowbridge.finance.domain.model.TransactionType;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class BudgetService implements BudgetUseCase {

    private final BudgetRepository budgetRepository;
    private final TransactionRepository transactionRepository;
    private final BudgetEventPort budgetEventPort;

    public BudgetService(BudgetRepository budgetRepository,
                         TransactionRepository transactionRepository,
                         BudgetEventPort budgetEventPort) {
        this.budgetRepository = budgetRepository;
        this.transactionRepository = transactionRepository;
        this.budgetEventPort = budgetEventPort;
    }

    @Override
    public Budget createOrUpdateBudget(CreateBudgetCommand command) {
        // Upsert: if a budget already exists for this user+category, update it
        Optional<Budget> existing = budgetRepository.findByUserIdAndCategory(command.getUserId(), command.getCategory());

        Budget budget;
        if (existing.isPresent()) {
            budget = existing.get();
            budget.setLimitAmount(command.getLimitAmount());
            budget.setPeriod(command.getPeriod());
        } else {
            budget = Budget.builder()
                    .id(UUID.randomUUID())
                    .userId(command.getUserId())
                    .category(command.getCategory())
                    .limitAmount(command.getLimitAmount())
                    .period(command.getPeriod())
                    .createdAt(Instant.now())
                    .build();
        }

        Budget saved = budgetRepository.save(budget);
        
        budgetEventPort.publishBudgetCreated(
                saved.getId(),
                saved.getUserId(),
                saved.getCategory().name(),
                saved.getLimitAmount(),
                saved.getPeriod().name()
        );

        return saved;
    }

    @Override
    public void deleteBudget(UUID budgetId, UUID userId) {
        Budget budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found: " + budgetId));
        if (!budget.getUserId().equals(userId)) {
            throw new AuthenticationException("Unauthorized access to this budget");
        }
        budgetRepository.deleteById(budgetId);
    }

    @Override
    public Budget getBudgetById(UUID budgetId, UUID userId) {
        Budget budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found: " + budgetId));
        if (!budget.getUserId().equals(userId)) {
            throw new AuthenticationException("Unauthorized access to this budget");
        }
        return budget;
    }

    @Override
    public List<Budget> getBudgetsByUserId(UUID userId) {
        return budgetRepository.findByUserId(userId);
    }

    @Override
    public void checkBudget(UUID userId, Category category, BigDecimal newAmount) {
        Optional<Budget> budgetOpt = budgetRepository.findByUserIdAndCategory(userId, category);
        if (budgetOpt.isEmpty()) {
            return; // No budget set, no restriction
        }

        Budget budget = budgetOpt.get();

        // Sum all existing expenses in this category for the user
        BigDecimal currentSpending = transactionRepository.findByUserIdAndCategory(userId, category)
                .stream()
                .filter(t -> t.getType() == TransactionType.EXPENSE)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal projectedTotal = currentSpending.add(newAmount);

        if (projectedTotal.compareTo(budget.getLimitAmount()) > 0) {
            budgetEventPort.publishBudgetExceeded(userId, category.name(), projectedTotal, budget.getLimitAmount());
            throw new BudgetExceededException(category.name(), projectedTotal, budget.getLimitAmount());
        }
    }
}
