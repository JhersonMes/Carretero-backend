package com.carretero.dto;

import com.carretero.model.enums.TableStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DiningTableDTO {

    private Integer idTable;

    @NotBlank(message = "El nombre o número de mesa es obligatorio")
    @Size(max = 50, message = "El nombre no debe superar 50 caracteres")
    private String name;

    @NotNull(message = "La capacidad es obligatoria")
    @Min(value = 1, message = "La capacidad mínima es 1")
    private Integer capacity = 4;

    private TableStatus status = TableStatus.LIBRE;

    /** Posicion en el plano del salon. La define el administrador arrastrando las mesas. */
    private Integer orderIndex;

    private boolean active = true;
}
