package com.digitalfix.ms_digitalfix_report.repository;

import com.digitalfix.ms_digitalfix_report.model.OrderSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface OrderSnapshotRepository extends JpaRepository<OrderSnapshot, Long> {

    /** Una fila del reporte: una clave (estado o técnico) y cuántas órdenes tiene. */
    interface Conteo {
        String getClave();

        Long getTotal();
    }

    @Query("SELECT o.status AS clave, COUNT(o) AS total "
            + "FROM OrderSnapshot o GROUP BY o.status ORDER BY o.status")
    List<Conteo> contarPorEstado();

    @Query("SELECT o.technicianName AS clave, COUNT(o) AS total "
            + "FROM OrderSnapshot o WHERE o.technicianName IS NOT NULL "
            + "GROUP BY o.technicianName ORDER BY COUNT(o) DESC")
    List<Conteo> contarPorTecnico();
}