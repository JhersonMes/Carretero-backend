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
public class IngredientDTO {

    private Integer idIngredient;

    @NotNull(message = "El nombre del ingrediente es requerido")
    @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
    private String name;

    @NotNull(message = "La unidad de medida es requerida")
    @Size(min = 1, max = 20, message = "La unidad de medida debe tener entre 1 y 20 caracteres")
    private String unitOfMeasure;

    private String description;

    private Boolean status;

    @NotNull(message = "El costo unitario es requerido")
    @DecimalMin(value = "0.0", inclusive = true, message = "El costo unitario debe ser mayor o igual a 0")
    private BigDecimal unitCost;

    @NotNull(message = "La cantidad en stock es requerida")
    @DecimalMin(value = "0.0", inclusive = true, message = "El stock debe ser mayor o igual a 0")
    private BigDecimal stockQuantity;
}
