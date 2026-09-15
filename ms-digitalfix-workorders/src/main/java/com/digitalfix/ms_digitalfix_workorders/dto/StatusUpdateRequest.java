package com.digitalfix.ms_digitalfix_workorders.dto;

import com.digitalfix.ms_digitalfix_workorders.model.WorkOrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StatusUpdateRequest {

    @NotNull(message = "status es obligatorio")
    private WorkOrderStatus status;

    private String technicianName;
}