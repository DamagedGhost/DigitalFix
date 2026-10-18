package com.digitalfix.ms_digitalfix_notify.admin;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * API del administrador de colas.
 * flow = email | dispatch | report
 */
@RestController
@RequestMapping("/api/admin/queues")
public class AdminQueueController {

    private final AdminQueueService service;

    public AdminQueueController(AdminQueueService service) {
        this.service = service;
    }

    // Estado de las colas y DLQ del sistema
    @GetMapping
    public List<QueueStatus> listar() {
        return service.listar();
    }

    // Estado de una cola cualquiera por nombre
    @GetMapping("/{name}")
    public QueueStatus consultar(@PathVariable String name) {
        return service.consultar(name);
    }

    // Crear una cola nueva
    @PostMapping
    public ResponseEntity<QueueStatus> crear(@RequestBody CreateQueueRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(request));
    }

    // Borrar una cola (no deja borrar las del sistema)
    @DeleteMapping("/{name}")
    public Map<String, Object> borrar(@PathVariable String name) {
        service.borrar(name);
        return Map.of("queue", name, "deleted", true);
    }

    // Vaciar la cola principal (dlq=false) o la DLQ (dlq=true)
    @DeleteMapping("/{flow}/purge")
    public Map<String, Object> purgar(@PathVariable String flow,
                                      @RequestParam(defaultValue = "false") boolean dlq) {
        int borrados = service.purgar(flow, dlq);
        return Map.of("flow", flow, "dlq", dlq, "purged", borrados);
    }

    // Mover mensajes de la DLQ de vuelta a la cola principal
    @PostMapping("/{flow}/reprocess")
    public Map<String, Object> reprocesar(@PathVariable String flow,
                                          @RequestParam(defaultValue = "100") int max) {
        int movidos = service.reprocesar(flow, max);
        return Map.of("flow", flow, "reprocessed", movidos);
    }

    // Provocar fallo intencional para demostrar en vivo el desvío a DLQ
    @PostMapping("/simulate-dlq")
    public Map<String, Object> simularFalloDlq() {
        return service.simularFalloDlq();
    }
}