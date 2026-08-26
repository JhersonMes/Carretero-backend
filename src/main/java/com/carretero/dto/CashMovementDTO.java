package com.carretero.dto;

import com.carretero.model.enums.CashMovementType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CashMovementDTO {

    private Integer idMovement;
    private Integer idCashShift;
    private UserDTO user;

    @NotNull(message = "El tipo de movimiento es obligatorio (INGRESO, EGRESO)")
    private CashMovementType type = CashMovementType.EGRESO;

    @NotNull(message = "El monto es obligatorio")
    @DecimalMin(value = "0.01", message = "El monto debe ser mayor a 0")
    private BigDecimal amount;

    @NotBlank(message = "El motivo es obligatorio")
    private String reason;

    private LocalDateTime createdAt;
}
