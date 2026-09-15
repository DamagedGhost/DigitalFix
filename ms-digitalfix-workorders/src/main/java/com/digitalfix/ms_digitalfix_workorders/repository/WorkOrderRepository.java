package com.digitalfix.ms_digitalfix_workorders.repository;

import com.digitalfix.ms_digitalfix_workorders.model.WorkOrder;
import com.digitalfix.ms_digitalfix_workorders.model.WorkOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface WorkOrderRepository extends JpaRepository<WorkOrder, Long> {

    List<WorkOrder> findByStatus(WorkOrderStatus status);

    List<WorkOrder> findByCreatedAtBetween(LocalDateTime from, LocalDateTime to);

    List<WorkOrder> findByStatusAndCreatedAtBetween(
            WorkOrderStatus status, LocalDateTime from, LocalDateTime to);
}