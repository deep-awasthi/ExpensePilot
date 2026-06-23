package com.flowbridge.finance.infrastructure.persistence.adapter;

import com.flowbridge.finance.application.port.out.AuditEventRepository;
import com.flowbridge.finance.domain.model.AuditEvent;
import com.flowbridge.finance.infrastructure.persistence.entity.AuditEventEntity;
import com.flowbridge.finance.infrastructure.persistence.repository.JpaAuditEventRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class AuditEventRepositoryAdapter implements AuditEventRepository {

    private final JpaAuditEventRepository jpaAuditEventRepository;

    public AuditEventRepositoryAdapter(JpaAuditEventRepository jpaAuditEventRepository) {
        this.jpaAuditEventRepository = jpaAuditEventRepository;
    }

    @Override
    public AuditEvent save(AuditEvent event) {
        AuditEventEntity entity = AuditEventEntity.fromDomain(event);
        AuditEventEntity savedEntity = jpaAuditEventRepository.save(entity);
        return savedEntity.toDomain();
    }

    @Override
    public List<AuditEvent> findAll() {
        return jpaAuditEventRepository.findAll().stream()
                .map(AuditEventEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public long count() {
        return jpaAuditEventRepository.count();
    }
}
