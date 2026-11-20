package com.digitalfix.ms_digitalfix_report.controller;

import com.digitalfix.ms_digitalfix_report.service.ReportService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {

    /** Lo que devuelve el GET: el reporte completo en un solo JSON. */
    public record Reporte(
            long totalOrdenes,
            Map<String, Long> porEstado,
            Map<String, Long> porTecnico,
            String generadoEn) {
    }

    private final ReportService service;

    public ReportController(ReportService service) {
        this.service = service;
    }

    @GetMapping("/orders")
    public Reporte reporte() {
        return new Reporte(
                service.totalOrdenes(),
                service.porEstado(),
                service.porTecnico(),
                Instant.now().toString());
    }
}