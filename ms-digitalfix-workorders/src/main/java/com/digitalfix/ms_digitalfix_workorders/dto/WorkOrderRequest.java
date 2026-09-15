package com.digitalfix.ms_digitalfix_workorders.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WorkOrderRequest {

    @NotBlank(message = "clientName es obligatorio")
    private String clientName;

    @NotBlank(message = "description es obligatorio")
    private String description;
}