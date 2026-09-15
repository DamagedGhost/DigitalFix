package com.digitalfix.ms_digitalfix_workorders.service;

import com.digitalfix.ms_digitalfix_workorders.dto.StatusUpdateRequest;
import com.digitalfix.ms_digitalfix_workorders.dto.WorkOrderRequest;
import com.digitalfix.ms_digitalfix_workorders.model.WorkOrder;
import com.digitalfix.ms_digitalfix_workorders.model.WorkOrderStatus;
import com.digitalfix.ms_digitalfix_workorders.repository.WorkOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias de WorkOrderService.
 *
 * Usa los valores reales de WorkOrderStatus: CREADA, ASIGNADA,
 * EN_DESPLAZAMIENTO, EN_EJECUCION, CERRADA, CANCELADA.
 */
@ExtendWith(MockitoExtension.class)
class WorkOrderServiceTest {

    @Mock
    private WorkOrderRepository repository;

    @InjectMocks
    private WorkOrderService service;

    private WorkOrder existingOrder;

    @BeforeEach
    void setUp() {
        existingOrder = new WorkOrder();
        existingOrder.setId(1L);
        existingOrder.setClientName("Cliente Test");
        existingOrder.setDescription("Descripcion test");
    }

    // ---------- create() ----------

    @Test
    void create_deberiaGuardarConEstadoCreada() {
        WorkOrderRequest request = new WorkOrderRequest();
        request.setClientName("Juan Perez");
        request.setDescription("Reparar impresora");

        when(repository.save(any(WorkOrder.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        WorkOrder result = service.create(request);

        assertThat(result.getClientName()).isEqualTo("Juan Perez");
        assertThat(result.getDescription()).isEqualTo("Reparar impresora");
        assertThat(result.getStatus()).isEqualTo(WorkOrderStatus.CREADA);
        verify(repository, times(1)).save(any(WorkOrder.class));
    }

    // ---------- findById() ----------

    @Test
    void findById_deberiaRetornarLaOrdenSiExiste() {
        when(repository.findById(1L)).thenReturn(Optional.of(existingOrder));

        WorkOrder result = service.findById(1L);

        assertThat(result).isEqualTo(existingOrder);
    }

    @Test
    void findById_deberiaLanzarExcepcionSiNoExiste() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("99");
    }

    // ---------- findAll() ----------

    @Test
    void findAll_sinFiltros_deberiaLlamarFindAll() {
        when(repository.findAll()).thenReturn(List.of(existingOrder));

        List<WorkOrder> result = service.findAll(null, null, null);

        assertThat(result).containsExactly(existingOrder);
        verify(repository, times(1)).findAll();
        verify(repository, never()).findByStatus(any());
    }

    @Test
    void findAll_soloConStatus_deberiaLlamarFindByStatus() {
        when(repository.findByStatus(WorkOrderStatus.CREADA))
                .thenReturn(List.of(existingOrder));

        List<WorkOrder> result = service.findAll(WorkOrderStatus.CREADA, null, null);

        assertThat(result).containsExactly(existingOrder);
        verify(repository, times(1)).findByStatus(WorkOrderStatus.CREADA);
        verify(repository, never()).findAll();
    }

    // ---------- updateStatus(): la regla de negocio clave ----------

    @Test
    void updateStatus_deCreadaAEnEjecucion_deberiaLanzarIllegalStateException() {
        existingOrder.setStatus(WorkOrderStatus.CREADA);
        when(repository.findById(1L)).thenReturn(Optional.of(existingOrder));

        StatusUpdateRequest request = new StatusUpdateRequest();
        request.setStatus(WorkOrderStatus.EN_EJECUCION);

        assertThatThrownBy(() -> service.updateStatus(1L, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ASIGNAR");

        // No debe haber intentado guardar el cambio inv\u00e1lido
        verify(repository, never()).save(any(WorkOrder.class));
    }

    @Test
    void updateStatus_deAsignadaAEnEjecucion_deberiaPermitirse() {
        existingOrder.setStatus(WorkOrderStatus.ASIGNADA);
        when(repository.findById(1L)).thenReturn(Optional.of(existingOrder));
        when(repository.save(any(WorkOrder.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        StatusUpdateRequest request = new StatusUpdateRequest();
        request.setStatus(WorkOrderStatus.EN_EJECUCION);
        request.setTechnicianName("Pedro Soto");

        WorkOrder result = service.updateStatus(1L, request);

        assertThat(result.getStatus()).isEqualTo(WorkOrderStatus.EN_EJECUCION);
        assertThat(result.getTechnicianName()).isEqualTo("Pedro Soto");
        verify(repository, times(1)).save(existingOrder);
    }

    @Test
    void updateStatus_conTechnicianNameNulo_noDeberiaSobrescribirElExistente() {
        existingOrder.setStatus(WorkOrderStatus.ASIGNADA);
        existingOrder.setTechnicianName("Tecnico Original");
        when(repository.findById(1L)).thenReturn(Optional.of(existingOrder));
        when(repository.save(any(WorkOrder.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        StatusUpdateRequest request = new StatusUpdateRequest();
        request.setStatus(WorkOrderStatus.CERRADA);
        request.setTechnicianName(null);

        WorkOrder result = service.updateStatus(1L, request);

        assertThat(result.getStatus()).isEqualTo(WorkOrderStatus.CERRADA);
        assertThat(result.getTechnicianName()).isEqualTo("Tecnico Original");
    }

    @Test
    void updateStatus_ordenInexistente_deberiaLanzarIllegalArgumentException() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        StatusUpdateRequest request = new StatusUpdateRequest();
        request.setStatus(WorkOrderStatus.ASIGNADA);

        assertThatThrownBy(() -> service.updateStatus(99L, request))
                .isInstanceOf(IllegalArgumentException.class);

        verify(repository, never()).save(any(WorkOrder.class));
    }
}