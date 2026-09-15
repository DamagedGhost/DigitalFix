package com.digitalfix.ms_digitalfix_workorders.service;

import com.digitalfix.ms_digitalfix_workorders.dto.StatusUpdateRequest;
import com.digitalfix.ms_digitalfix_workorders.dto.WorkOrderRequest;
import com.digitalfix.ms_digitalfix_workorders.model.WorkOrder;
import com.digitalfix.ms_digitalfix_workorders.model.WorkOrderStatus;
import com.digitalfix.ms_digitalfix_workorders.repository.WorkOrderRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class WorkOrderService {

    private final WorkOrderRepository repository;

    public WorkOrderService(WorkOrderRepository repository) {
        this.repository = repository;
    }

    public WorkOrder create(WorkOrderRequest request) {
        WorkOrder order = new WorkOrder();
        order.setClientName(request.getClientName());
        order.setDescription(request.getDescription());
        order.setStatus(WorkOrderStatus.CREADA);
        return repository.save(order);
    }

    public WorkOrder findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Orden no encontrada: " + id));
    }

    public List<WorkOrder> findAll(WorkOrderStatus status, LocalDateTime from, LocalDateTime to) {
        if (status != null && from != null && to != null) {
            return repository.findByStatusAndCreatedAtBetween(status, from, to);
        }
        if (status != null) {
            return repository.findByStatus(status);
        }
        if (from != null && to != null) {
            return repository.findByCreatedAtBetween(from, to);
        }
        return repository.findAll();
    }

    public WorkOrder updateStatus(Long id, StatusUpdateRequest request) {
        WorkOrder order = findById(id);
        WorkOrderStatus current = order.getStatus();
        WorkOrderStatus next = request.getStatus();

        if (next == WorkOrderStatus.EN_EJECUCION && current == WorkOrderStatus.CREADA) {
            throw new IllegalStateException(
                    "No se puede pasar a EN EJECUCION sin ASIGNAR la orden primero");
        }

        order.setStatus(next);
        if (request.getTechnicianName() != null) {
            order.setTechnicianName(request.getTechnicianName());
        }
        return repository.save(order);
    }
}