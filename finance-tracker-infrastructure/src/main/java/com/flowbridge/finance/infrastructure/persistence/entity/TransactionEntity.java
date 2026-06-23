package com.flowbridge.finance.infrastructure.persistence.entity;

import com.flowbridge.finance.domain.model.Category;
import com.flowbridge.finance.domain.model.Transaction;
import com.flowbridge.finance.domain.model.TransactionType;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transactions")
public class TransactionEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private Category category;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(length = 255)
    private String description;

    @Column(name = "transaction_date", nullable = false)
    private Instant transactionDate;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public TransactionEntity() {
    }

    public TransactionEntity(UUID id, UUID userId, TransactionType type, Category category, BigDecimal amount, String description, Instant transactionDate, Instant createdAt) {
        this.id = id;
        this.userId = userId;
        this.type = type;
        this.category = category;
        this.amount = amount;
        this.description = description;
        this.transactionDate = transactionDate;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public TransactionType getType() {
        return type;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Instant getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(Instant transactionDate) {
        this.transactionDate = transactionDate;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Transaction toDomain() {
        return Transaction.builder()
                .id(this.id)
                .userId(this.userId)
                .type(this.type)
                .category(this.category)
                .amount(this.amount)
                .description(this.description)
                .transactionDate(this.transactionDate)
                .createdAt(this.createdAt)
                .build();
    }

    public static TransactionEntity fromDomain(Transaction domain) {
        if (domain == null) {
            return null;
        }
        return new TransactionEntity(
                domain.getId(),
                domain.getUserId(),
                domain.getType(),
                domain.getCategory(),
                domain.getAmount(),
                domain.getDescription(),
                domain.getTransactionDate(),
                domain.getCreatedAt()
        );
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID id;
        private UUID userId;
        private TransactionType type;
        private Category category;
        private BigDecimal amount;
        private String description;
        private Instant transactionDate;
        private Instant createdAt;

        public Builder id(UUID id) {
            this.id = id;
            return this;
        }

        public Builder userId(UUID userId) {
            this.userId = userId;
            return this;
        }

        public Builder type(TransactionType type) {
            this.type = type;
            return this;
        }

        public Builder category(Category category) {
            this.category = category;
            return this;
        }

        public Builder amount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder transactionDate(Instant transactionDate) {
            this.transactionDate = transactionDate;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public TransactionEntity build() {
            return new TransactionEntity(id, userId, type, category, amount, description, transactionDate, createdAt);
        }
    }
}
