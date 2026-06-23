package com.flowbridge.finance.infrastructure.persistence.repository;

import com.flowbridge.finance.infrastructure.persistence.entity.AuditEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface JpaAuditEventRepository extends JpaRepository<AuditEventEntity, UUID> {
}
