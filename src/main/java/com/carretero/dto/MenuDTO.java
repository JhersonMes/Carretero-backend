package com.carretero.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MenuDTO {

    private Integer idMenu;

    @NotNull(message = "El nombre del menu es requerido")
    @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
    private String name;

    @NotNull(message = "La cantidad de platos es requerida")
    private Integer dishQuantity;

    private String description;

    @NotNull(message = "El precio es requerido")
    @DecimalMin(value = "0.0", inclusive = true, message = "El precio debe ser mayor o igual a 0")
    private BigDecimal price;

    private Boolean status;
}
