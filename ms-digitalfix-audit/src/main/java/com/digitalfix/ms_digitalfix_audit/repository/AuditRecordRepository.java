package com.digitalfix.ms_digitalfix_audit.repository;

import com.digitalfix.ms_digitalfix_audit.model.AuditRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditRecordRepository extends JpaRepository<AuditRecord, Long> {

    boolean existsByAuditId(String auditId);

    List<AuditRecord> findTop100ByOrderByIdDesc();

    List<AuditRecord> findByOutcomeOrderByIdDesc(String outcome);

    List<AuditRecord> findByOrderIdOrderByIdDesc(Long orderId);
}