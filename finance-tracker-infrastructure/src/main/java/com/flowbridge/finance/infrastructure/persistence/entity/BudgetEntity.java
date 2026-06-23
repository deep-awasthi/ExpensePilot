package com.flowbridge.finance.infrastructure.persistence.entity;

import com.flowbridge.finance.domain.model.Budget;
import com.flowbridge.finance.domain.model.BudgetPeriod;
import com.flowbridge.finance.domain.model.Category;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "budgets",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "category"}))
public class BudgetEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private Category category;

    @Column(name = "limit_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal limitAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BudgetPeriod period;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public BudgetEntity() {}

    public BudgetEntity(UUID id, UUID userId, Category category, BigDecimal limitAmount, BudgetPeriod period, Instant createdAt) {
        this.id = id;
        this.userId = userId;
        this.category = category;
        this.limitAmount = limitAmount;
        this.period = period;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    public BigDecimal getLimitAmount() { return limitAmount; }
    public void setLimitAmount(BigDecimal limitAmount) { this.limitAmount = limitAmount; }
    public BudgetPeriod getPeriod() { return period; }
    public void setPeriod(BudgetPeriod period) { this.period = period; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Budget toDomain() {
        return Budget.builder()
                .id(this.id).userId(this.userId).category(this.category)
                .limitAmount(this.limitAmount).period(this.period).createdAt(this.createdAt)
                .build();
    }

    public static BudgetEntity fromDomain(Budget b) {
        if (b == null) return null;
        return new BudgetEntity(b.getId(), b.getUserId(), b.getCategory(),
                b.getLimitAmount(), b.getPeriod(), b.getCreatedAt());
    }
}
