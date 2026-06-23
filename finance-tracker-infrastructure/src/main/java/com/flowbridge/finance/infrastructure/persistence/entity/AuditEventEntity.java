package com.flowbridge.finance.infrastructure.persistence.entity;

import com.flowbridge.finance.domain.model.AuditEvent;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_events")
public class AuditEventEntity {

    @Id
    private UUID id;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "aggregate_id", length = 100)
    private String aggregateId;

    @Column(columnDefinition = "TEXT")
    private String payload;

    @Column(nullable = false)
    private Instant timestamp;

    public AuditEventEntity() {
    }

    public AuditEventEntity(UUID id, String eventType, String aggregateId, String payload, Instant timestamp) {
        this.id = id;
        this.eventType = eventType;
        this.aggregateId = aggregateId;
        this.payload = payload;
        this.timestamp = timestamp;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getAggregateId() {
        return aggregateId;
    }

    public void setAggregateId(String aggregateId) {
        this.aggregateId = aggregateId;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public AuditEvent toDomain() {
        return AuditEvent.builder()
                .id(this.id)
                .eventType(this.eventType)
                .aggregateId(this.aggregateId)
                .payload(this.payload)
                .timestamp(this.timestamp)
                .build();
    }

    public static AuditEventEntity fromDomain(AuditEvent domain) {
        if (domain == null) {
            return null;
        }
        return AuditEventEntity.builder()
                .id(domain.getId())
                .eventType(domain.getEventType())
                .aggregateId(domain.getAggregateId())
                .payload(domain.getPayload())
                .timestamp(domain.getTimestamp())
                .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID id;
        private String eventType;
        private String aggregateId;
        private String payload;
        private Instant timestamp;

        public Builder id(UUID id) {
            this.id = id;
            return this;
        }

        public Builder eventType(String eventType) {
            this.eventType = eventType;
            return this;
        }

        public Builder aggregateId(String aggregateId) {
            this.aggregateId = aggregateId;
            return this;
        }

        public Builder payload(String payload) {
            this.payload = payload;
            return this;
        }

        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public AuditEventEntity build() {
            return new AuditEventEntity(id, eventType, aggregateId, payload, timestamp);
        }
    }
}
