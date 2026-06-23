package com.flowbridge.finance.application.service;

import com.flowbridge.finance.application.port.in.CreateBudgetCommand;
import com.flowbridge.finance.application.port.out.BudgetRepository;
import com.flowbridge.finance.application.port.out.TransactionRepository;
import com.flowbridge.finance.domain.exception.AuthenticationException;
import com.flowbridge.finance.domain.exception.BudgetExceededException;
import com.flowbridge.finance.domain.exception.ResourceNotFoundException;
import com.flowbridge.finance.domain.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BudgetServiceTest {

    private BudgetRepository budgetRepository;
    private TransactionRepository transactionRepository;
    private com.flowbridge.finance.application.port.out.BudgetEventPort budgetEventPort;
    private BudgetService budgetService;

    @BeforeEach
    void setUp() {
        budgetRepository = mock(BudgetRepository.class);
        transactionRepository = mock(TransactionRepository.class);
        budgetEventPort = mock(com.flowbridge.finance.application.port.out.BudgetEventPort.class);
        budgetService = new BudgetService(budgetRepository, transactionRepository, budgetEventPort);
    }

    @Test
    void createOrUpdateBudget_shouldCreateNew_whenNoneExists() {
        UUID userId = UUID.randomUUID();
        CreateBudgetCommand cmd = new CreateBudgetCommand(userId, Category.FOOD, new BigDecimal("500.00"), BudgetPeriod.MONTHLY);

        when(budgetRepository.findByUserIdAndCategory(userId, Category.FOOD)).thenReturn(Optional.empty());
        when(budgetRepository.save(any(Budget.class))).thenAnswer(i -> i.getArgument(0));

        Budget result = budgetService.createOrUpdateBudget(cmd);

        assertNotNull(result.getId());
        assertEquals(Category.FOOD, result.getCategory());
        assertEquals(new BigDecimal("500.00"), result.getLimitAmount());
        assertEquals(BudgetPeriod.MONTHLY, result.getPeriod());
        verify(budgetRepository).save(any(Budget.class));
    }

    @Test
    void createOrUpdateBudget_shouldUpdate_whenAlreadyExists() {
        UUID userId = UUID.randomUUID();
        UUID existingId = UUID.randomUUID();
        Budget existing = Budget.builder()
                .id(existingId).userId(userId).category(Category.FOOD)
                .limitAmount(new BigDecimal("300.00")).period(BudgetPeriod.MONTHLY)
                .createdAt(Instant.now()).build();

        CreateBudgetCommand cmd = new CreateBudgetCommand(userId, Category.FOOD, new BigDecimal("600.00"), BudgetPeriod.YEARLY);

        when(budgetRepository.findByUserIdAndCategory(userId, Category.FOOD)).thenReturn(Optional.of(existing));
        when(budgetRepository.save(any(Budget.class))).thenAnswer(i -> i.getArgument(0));

        Budget result = budgetService.createOrUpdateBudget(cmd);

        // ID must remain the same (update, not create)
        assertEquals(existingId, result.getId());
        assertEquals(new BigDecimal("600.00"), result.getLimitAmount());
        assertEquals(BudgetPeriod.YEARLY, result.getPeriod());
    }

    @Test
    void deleteBudget_shouldDelete_whenOwner() {
        UUID userId = UUID.randomUUID();
        UUID budgetId = UUID.randomUUID();
        Budget budget = Budget.builder().id(budgetId).userId(userId).build();

        when(budgetRepository.findById(budgetId)).thenReturn(Optional.of(budget));

        budgetService.deleteBudget(budgetId, userId);

        verify(budgetRepository).deleteById(budgetId);
    }

    @Test
    void deleteBudget_shouldThrow_whenNotOwner() {
        UUID ownerId = UUID.randomUUID();
        UUID otherId = UUID.randomUUID();
        UUID budgetId = UUID.randomUUID();
        Budget budget = Budget.builder().id(budgetId).userId(ownerId).build();

        when(budgetRepository.findById(budgetId)).thenReturn(Optional.of(budget));

        assertThrows(AuthenticationException.class, () -> budgetService.deleteBudget(budgetId, otherId));
        verify(budgetRepository, never()).deleteById(any());
    }

    @Test
    void checkBudget_shouldPass_whenNoBudgetSet() {
        UUID userId = UUID.randomUUID();
        when(budgetRepository.findByUserIdAndCategory(userId, Category.FOOD)).thenReturn(Optional.empty());

        // Should not throw
        assertDoesNotThrow(() -> budgetService.checkBudget(userId, Category.FOOD, new BigDecimal("1000.00")));
    }

    @Test
    void checkBudget_shouldPass_whenUnderLimit() {
        UUID userId = UUID.randomUUID();
        Budget budget = Budget.builder().id(UUID.randomUUID()).userId(userId)
                .category(Category.FOOD).limitAmount(new BigDecimal("500.00"))
                .period(BudgetPeriod.MONTHLY).createdAt(Instant.now()).build();

        Transaction existing = Transaction.builder()
                .id(UUID.randomUUID()).userId(userId)
                .type(TransactionType.EXPENSE).category(Category.FOOD)
                .amount(new BigDecimal("200.00")).build();

        when(budgetRepository.findByUserIdAndCategory(userId, Category.FOOD)).thenReturn(Optional.of(budget));
        when(transactionRepository.findByUserIdAndCategory(userId, Category.FOOD)).thenReturn(List.of(existing));

        // 200 existing + 100 new = 300, under 500 limit
        assertDoesNotThrow(() -> budgetService.checkBudget(userId, Category.FOOD, new BigDecimal("100.00")));
    }

    @Test
    void checkBudget_shouldThrow_whenOverLimit() {
        UUID userId = UUID.randomUUID();
        Budget budget = Budget.builder().id(UUID.randomUUID()).userId(userId)
                .category(Category.FOOD).limitAmount(new BigDecimal("500.00"))
                .period(BudgetPeriod.MONTHLY).createdAt(Instant.now()).build();

        Transaction existing = Transaction.builder()
                .id(UUID.randomUUID()).userId(userId)
                .type(TransactionType.EXPENSE).category(Category.FOOD)
                .amount(new BigDecimal("450.00")).build();

        when(budgetRepository.findByUserIdAndCategory(userId, Category.FOOD)).thenReturn(Optional.of(budget));
        when(transactionRepository.findByUserIdAndCategory(userId, Category.FOOD)).thenReturn(List.of(existing));

        // 450 existing + 100 new = 550, over 500 limit — must throw
        BudgetExceededException ex = assertThrows(BudgetExceededException.class,
                () -> budgetService.checkBudget(userId, Category.FOOD, new BigDecimal("100.00")));

        assertEquals("FOOD", ex.getCategory());
        assertEquals(new BigDecimal("550.00"), ex.getSpent());
        assertEquals(new BigDecimal("500.00"), ex.getLimit());
    }
}
