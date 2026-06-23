package com.flowbridge.finance.infrastructure.persistence.repository;

import com.flowbridge.finance.domain.model.Category;
import com.flowbridge.finance.infrastructure.persistence.entity.BudgetEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaBudgetRepository extends JpaRepository<BudgetEntity, UUID> {
    Optional<BudgetEntity> findByUserIdAndCategory(UUID userId, Category category);
    List<BudgetEntity> findByUserId(UUID userId);
}
