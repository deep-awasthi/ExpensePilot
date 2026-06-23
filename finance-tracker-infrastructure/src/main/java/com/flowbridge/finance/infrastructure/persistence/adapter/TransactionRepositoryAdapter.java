package com.flowbridge.finance.infrastructure.persistence.adapter;

import com.flowbridge.finance.application.port.out.TransactionRepository;
import com.flowbridge.finance.domain.model.Category;
import com.flowbridge.finance.domain.model.Transaction;
import com.flowbridge.finance.infrastructure.persistence.entity.TransactionEntity;
import com.flowbridge.finance.infrastructure.persistence.repository.JpaTransactionRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class TransactionRepositoryAdapter implements TransactionRepository {

    private final JpaTransactionRepository jpaTransactionRepository;

    public TransactionRepositoryAdapter(JpaTransactionRepository jpaTransactionRepository) {
        this.jpaTransactionRepository = jpaTransactionRepository;
    }

    @Override
    public Transaction save(Transaction transaction) {
        TransactionEntity entity = TransactionEntity.fromDomain(transaction);
        TransactionEntity savedEntity = jpaTransactionRepository.save(entity);
        return savedEntity.toDomain();
    }

    @Override
    public Optional<Transaction> findById(UUID id) {
        return jpaTransactionRepository.findById(id).map(TransactionEntity::toDomain);
    }

    @Override
    public List<Transaction> findByUserId(UUID userId) {
        return jpaTransactionRepository.findByUserId(userId).stream()
                .map(TransactionEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Transaction> findByUserIdAndCategory(UUID userId, Category category) {
        return jpaTransactionRepository.findByUserIdAndCategory(userId, category).stream()
                .map(TransactionEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(UUID id) {
        jpaTransactionRepository.deleteById(id);
    }

    @Override
    public boolean existsById(UUID id) {
        return jpaTransactionRepository.existsById(id);
    }
}
