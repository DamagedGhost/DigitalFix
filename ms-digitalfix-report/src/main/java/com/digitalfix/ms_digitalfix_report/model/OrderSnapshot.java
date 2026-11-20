package com.digitalfix.ms_digitalfix_report.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "order_snapshots")
@Getter
@Setter
@NoArgsConstructor
public class OrderSnapshot {

    @Id
    private Long orderId;

    private String clientName;
    private String technicianName;
    private String status;
    private String lastEventId;
    private LocalDateTime updatedAt;
}