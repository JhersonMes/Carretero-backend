package com.carretero.dto;

import com.carretero.model.enums.KitchenStation;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductDTO {

    private Integer idProduct;

    @NotBlank(message = "El nombre del producto es obligatorio")
    @Size(max = 120, message = "El nombre no debe superar 120 caracteres")
    private String name;

    @Size(max = 255, message = "La descripción no debe superar 255 caracteres")
    private String description;

    @NotNull(message = "El precio es obligatorio")
    @DecimalMin(value = "0.0", inclusive = false, message = "El precio debe ser mayor a 0")
    private BigDecimal price;

    @NotNull(message = "La categoría es obligatoria")
    private CategoryDTO category;

    private KitchenStation station;

    private boolean requiresKitchen = true;

    private boolean requiresFlavor = false;

    private boolean active = true;

    private boolean manageStock = false;

    private BigDecimal stock = BigDecimal.ZERO;

    private String imageUrl;
}
