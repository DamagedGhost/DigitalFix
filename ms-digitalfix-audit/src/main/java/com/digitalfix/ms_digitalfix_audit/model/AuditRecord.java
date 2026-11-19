package com.digitalfix.ms_digitalfix_audit.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_records")
@Getter
@Setter
@NoArgsConstructor
public class AuditRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String auditId;
    private String eventTimestamp;
    private String sourceService;
    private String flow;
    private String outcome;
    private String reason;
    private Long orderId;
    private String originalEventId;
    private String originalType;
    private String traceId;
    private String correlationId;
    private LocalDateTime receivedAt;
}