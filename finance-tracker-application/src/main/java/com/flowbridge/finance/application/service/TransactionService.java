package com.flowbridge.finance.application.service;

import com.flowbridge.finance.application.port.in.CreateTransactionCommand;
import com.flowbridge.finance.application.port.in.TransactionUseCase;
import com.flowbridge.finance.application.port.in.UpdateTransactionCommand;
import com.flowbridge.finance.application.port.in.BudgetUseCase;
import com.flowbridge.finance.application.port.out.TransactionEventPort;
import com.flowbridge.finance.application.port.out.TransactionRepository;
import com.flowbridge.finance.domain.exception.AuthenticationException;
import com.flowbridge.finance.domain.exception.ResourceNotFoundException;
import com.flowbridge.finance.domain.model.Category;
import com.flowbridge.finance.domain.model.Transaction;
import com.flowbridge.finance.domain.model.TransactionType;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class TransactionService implements TransactionUseCase {

    private final TransactionRepository transactionRepository;
    private final BudgetUseCase budgetUseCase;
    private final TransactionEventPort transactionEventPort;

    public TransactionService(TransactionRepository transactionRepository,
                              @Lazy BudgetUseCase budgetUseCase,
                              TransactionEventPort transactionEventPort) {
        this.transactionRepository = transactionRepository;
        this.budgetUseCase = budgetUseCase;
        this.transactionEventPort = transactionEventPort;
    }

    @Override
    public Transaction createTransaction(CreateTransactionCommand command) {
        // Check budget limit before persisting EXPENSE transactions
        if (command.getType() == TransactionType.EXPENSE) {
            budgetUseCase.checkBudget(command.getUserId(), command.getCategory(), command.getAmount());
        }

        Transaction transaction = Transaction.builder()
                .id(UUID.randomUUID())
                .userId(command.getUserId())
                .type(command.getType())
                .category(command.getCategory())
                .amount(command.getAmount())
                .description(command.getDescription())
                .transactionDate(command.getTransactionDate())
                .createdAt(Instant.now())
                .build();

        Transaction saved = transactionRepository.save(transaction);

        transactionEventPort.publishTransactionCreated(
                saved.getId(),
                saved.getUserId(),
                saved.getType().name(),
                saved.getCategory().name(),
                saved.getAmount(),
                saved.getDescription()
        );

        return saved;
    }

    @Override
    public Transaction updateTransaction(UUID transactionId, UpdateTransactionCommand command) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with ID: " + transactionId));

        if (!transaction.getUserId().equals(command.getUserId())) {
            throw new AuthenticationException("Unauthorized access to this transaction");
        }

        transaction.setType(command.getType());
        transaction.setCategory(command.getCategory());
        transaction.setAmount(command.getAmount());
        transaction.setDescription(command.getDescription());
        transaction.setTransactionDate(command.getTransactionDate());

        return transactionRepository.save(transaction);
    }

    @Override
    public void deleteTransaction(UUID transactionId, UUID userId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with ID: " + transactionId));

        if (!transaction.getUserId().equals(userId)) {
            throw new AuthenticationException("Unauthorized access to this transaction");
        }

        transactionRepository.deleteById(transactionId);
    }

    @Override
    public Transaction getTransactionById(UUID transactionId, UUID userId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with ID: " + transactionId));

        if (!transaction.getUserId().equals(userId)) {
            throw new AuthenticationException("Unauthorized access to this transaction");
        }

        return transaction;
    }

    @Override
    public List<Transaction> getTransactionsByUserId(UUID userId) {
        return transactionRepository.findByUserId(userId);
    }

    @Override
    public List<Transaction> getTransactionsByUserIdAndCategory(UUID userId, Category category) {
        return transactionRepository.findByUserIdAndCategory(userId, category);
    }
}
