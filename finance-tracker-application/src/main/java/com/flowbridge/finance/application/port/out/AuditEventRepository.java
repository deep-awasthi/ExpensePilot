package com.flowbridge.finance.application.port.out;

import com.flowbridge.finance.domain.model.AuditEvent;
import java.util.List;

public interface AuditEventRepository {
    AuditEvent save(AuditEvent event);
    List<AuditEvent> findAll();
    long count();
}
