package com.digitalfix.ms_digitalfix_catalog.dto;

import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class StockUpdateRequest {

    @PositiveOrZero(message = "El precio no puede ser negativo")
    private BigDecimal price;

    @PositiveOrZero(message = "El stock no puede ser negativo")
    private Integer stock;
}