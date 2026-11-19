package com.digitalfix.ms_digitalfix_audit.controller;

import com.digitalfix.ms_digitalfix_audit.model.AuditRecord;
import com.digitalfix.ms_digitalfix_audit.service.AuditService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/audit")
public class AuditController {

    private final AuditService service;

    public AuditController(AuditService service) {
        this.service = service;
    }

    // GET /api/v1/audit?outcome=ENVIADO_A_DLQ  o  ?orderId=5  (sin filtros: los últimos 100)
    @GetMapping
    public List<AuditRecord> listar(@RequestParam(required = false) String outcome,
                                    @RequestParam(required = false) Long orderId) {
        return service.listar(outcome, orderId);
    }
}