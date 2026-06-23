package com.flowbridge.finance.application.service;

import com.flowbridge.finance.application.port.in.BudgetUseCase;
import com.flowbridge.finance.application.port.in.CreateTransactionCommand;
import com.flowbridge.finance.application.port.in.UpdateTransactionCommand;
import com.flowbridge.finance.application.port.out.TransactionRepository;
import com.flowbridge.finance.domain.exception.AuthenticationException;
import com.flowbridge.finance.domain.exception.ResourceNotFoundException;
import com.flowbridge.finance.domain.model.Category;
import com.flowbridge.finance.domain.model.Transaction;
import com.flowbridge.finance.domain.model.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TransactionServiceTest {

    private TransactionRepository transactionRepository;
    private BudgetUseCase budgetUseCase;
    private com.flowbridge.finance.application.port.out.TransactionEventPort transactionEventPort;
    private TransactionService transactionService;

    @BeforeEach
    void setUp() {
        transactionRepository = mock(TransactionRepository.class);
        budgetUseCase = mock(BudgetUseCase.class);
        transactionEventPort = mock(com.flowbridge.finance.application.port.out.TransactionEventPort.class);
        // By default checkBudget is void and does nothing (no exception)
        transactionService = new TransactionService(transactionRepository, budgetUseCase, transactionEventPort);
    }

    @Test
    void createTransaction_shouldSaveAndReturnTransaction() {
        // Given
        UUID userId = UUID.randomUUID();
        CreateTransactionCommand command = new CreateTransactionCommand(
                userId,
                TransactionType.EXPENSE,
                Category.FOOD,
                new BigDecimal("49.99"),
                "Weekly groceries",
                Instant.now()
        );

        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Transaction result = transactionService.createTransaction(command);

        // Then
        assertNotNull(result);
        assertNotNull(result.getId());
        assertEquals(userId, result.getUserId());
        assertEquals(TransactionType.EXPENSE, result.getType());
        assertEquals(Category.FOOD, result.getCategory());
        assertEquals(new BigDecimal("49.99"), result.getAmount());
        assertEquals("Weekly groceries", result.getDescription());
        assertNotNull(result.getCreatedAt());

        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void updateTransaction_shouldModifyAndSave_whenOwner() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();
        Transaction existing = Transaction.builder()
                .id(transactionId)
                .userId(userId)
                .type(TransactionType.EXPENSE)
                .category(Category.OTHER)
                .amount(new BigDecimal("10.00"))
                .description("Old desc")
                .transactionDate(Instant.now())
                .build();

        UpdateTransactionCommand command = new UpdateTransactionCommand(
                userId,
                TransactionType.EXPENSE,
                Category.TRANSPORTATION,
                new BigDecimal("15.50"),
                "New desc",
                Instant.now()
        );

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(existing));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Transaction updated = transactionService.updateTransaction(transactionId, command);

        // Then
        assertNotNull(updated);
        assertEquals(Category.TRANSPORTATION, updated.getCategory());
        assertEquals(new BigDecimal("15.50"), updated.getAmount());
        assertEquals("New desc", updated.getDescription());

        verify(transactionRepository).save(existing);
    }

    @Test
    void updateTransaction_shouldThrowException_whenNotOwner() {
        // Given
        UUID ownerId = UUID.randomUUID();
        UUID unauthorizedUserId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();
        Transaction existing = Transaction.builder()
                .id(transactionId)
                .userId(ownerId)
                .build();

        UpdateTransactionCommand command = new UpdateTransactionCommand(
                unauthorizedUserId,
                TransactionType.EXPENSE,
                Category.TRANSPORTATION,
                new BigDecimal("15.50"),
                "New desc",
                Instant.now()
        );

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(existing));

        // When/Then
        assertThrows(AuthenticationException.class, () -> transactionService.updateTransaction(transactionId, command));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void updateTransaction_shouldThrowException_whenNotFound() {
        // Given
        UUID transactionId = UUID.randomUUID();
        UpdateTransactionCommand command = new UpdateTransactionCommand(
                UUID.randomUUID(),
                TransactionType.EXPENSE,
                Category.TRANSPORTATION,
                new BigDecimal("15.50"),
                "New desc",
                Instant.now()
        );

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(ResourceNotFoundException.class, () -> transactionService.updateTransaction(transactionId, command));
    }

    @Test
    void deleteTransaction_shouldDelete_whenOwner() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();
        Transaction existing = Transaction.builder()
                .id(transactionId)
                .userId(userId)
                .build();

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(existing));

        // When
        transactionService.deleteTransaction(transactionId, userId);

        // Then
        verify(transactionRepository).deleteById(transactionId);
    }

    @Test
    void deleteTransaction_shouldThrowException_whenNotOwner() {
        // Given
        UUID ownerId = UUID.randomUUID();
        UUID unauthorizedUserId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();
        Transaction existing = Transaction.builder()
                .id(transactionId)
                .userId(ownerId)
                .build();

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(existing));

        // When/Then
        assertThrows(AuthenticationException.class, () -> transactionService.deleteTransaction(transactionId, unauthorizedUserId));
        verify(transactionRepository, never()).deleteById(any(UUID.class));
    }
}
