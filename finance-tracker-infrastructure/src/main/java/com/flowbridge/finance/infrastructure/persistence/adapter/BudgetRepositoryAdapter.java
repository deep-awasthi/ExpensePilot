package com.flowbridge.finance.infrastructure.persistence.adapter;

import com.flowbridge.finance.application.port.out.BudgetRepository;
import com.flowbridge.finance.domain.model.Budget;
import com.flowbridge.finance.domain.model.Category;
import com.flowbridge.finance.infrastructure.persistence.entity.BudgetEntity;
import com.flowbridge.finance.infrastructure.persistence.repository.JpaBudgetRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class BudgetRepositoryAdapter implements BudgetRepository {

    private final JpaBudgetRepository jpaBudgetRepository;

    public BudgetRepositoryAdapter(JpaBudgetRepository jpaBudgetRepository) {
        this.jpaBudgetRepository = jpaBudgetRepository;
    }

    @Override
    public Budget save(Budget budget) {
        BudgetEntity entity = BudgetEntity.fromDomain(budget);
        return jpaBudgetRepository.save(entity).toDomain();
    }

    @Override
    public Optional<Budget> findById(UUID id) {
        return jpaBudgetRepository.findById(id).map(BudgetEntity::toDomain);
    }

    @Override
    public Optional<Budget> findByUserIdAndCategory(UUID userId, Category category) {
        return jpaBudgetRepository.findByUserIdAndCategory(userId, category).map(BudgetEntity::toDomain);
    }

    @Override
    public List<Budget> findByUserId(UUID userId) {
        return jpaBudgetRepository.findByUserId(userId).stream()
                .map(BudgetEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(UUID id) {
        jpaBudgetRepository.deleteById(id);
    }
}
