package com.carretero.dto;

import com.carretero.model.enums.KitchenStation;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryDTO {

    private Integer idCategory;

    @NotBlank(message = "El nombre de la categoría es obligatorio")
    @Size(max = 80, message = "El nombre no debe superar 80 caracteres")
    private String name;

    @Size(max = 200, message = "La descripción no debe superar 200 caracteres")
    private String description;

    @NotNull(message = "La estación de preparación es obligatoria")
    private KitchenStation station = KitchenStation.COCINA;

    private Integer orderIndex = 0;

    private boolean active = true;
}
