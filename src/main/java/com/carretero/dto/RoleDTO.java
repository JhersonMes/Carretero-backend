package com.carretero.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoleDTO {

    private Integer idRole;

    @NotNull(message = "El nombre del rol es requerido")
    @Size(min = 2, max = 50, message = "El nombre debe tener entre 2 y 50 caracteres")
    private String name;

    @Size(max = 150, message = "La descripcion no debe exceder los 150 caracteres")
    private String description;
}
