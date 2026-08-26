package com.carretero.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentDTO {

    private Integer idPayment;
    private Integer idOrder;
    private Integer idCashShift;

    @NotNull(message = "El monto es requerido")
    @DecimalMin(value = "0.01", message = "El monto debe ser mayor a 0")
    private BigDecimal amount;

    private BigDecimal amountTendered;
    private BigDecimal changeGiven;

    @NotNull(message = "El método de pago es requerido")
    private PaymentMethodDTO paymentMethod;

    private UserDTO user;
    private String referenceNumber;
    private LocalDateTime paymentDate;
}
