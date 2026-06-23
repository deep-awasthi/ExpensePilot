package com.flowbridge.finance.application.port.out;

import com.flowbridge.finance.domain.model.Category;
import com.flowbridge.finance.domain.model.Transaction;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository {
    Transaction save(Transaction transaction);
    Optional<Transaction> findById(UUID id);
    List<Transaction> findByUserId(UUID userId);
    List<Transaction> findByUserIdAndCategory(UUID userId, Category category);
    void deleteById(UUID id);
    boolean existsById(UUID id);
}
