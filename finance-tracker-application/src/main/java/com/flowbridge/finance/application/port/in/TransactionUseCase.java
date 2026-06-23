package com.flowbridge.finance.application.port.in;

import com.flowbridge.finance.domain.model.Category;
import com.flowbridge.finance.domain.model.Transaction;

import java.util.List;
import java.util.UUID;

public interface TransactionUseCase {
    Transaction createTransaction(CreateTransactionCommand command);
    Transaction updateTransaction(UUID transactionId, UpdateTransactionCommand command);
    void deleteTransaction(UUID transactionId, UUID userId);
    Transaction getTransactionById(UUID transactionId, UUID userId);
    List<Transaction> getTransactionsByUserId(UUID userId);
    List<Transaction> getTransactionsByUserIdAndCategory(UUID userId, Category category);
}
