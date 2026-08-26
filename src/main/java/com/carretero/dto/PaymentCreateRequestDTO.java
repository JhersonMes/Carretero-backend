package com.carretero.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCreateRequestDTO {

    @NotNull(message = "El id de pedido es obligatorio")
    private Integer idOrder;

    @NotEmpty(message = "Debe registrar al menos un método de pago")
    @Valid
    private List<PaymentItemDTO> payments;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentItemDTO {
        @NotNull(message = "El id del método de pago es obligatorio")
        private Integer idPaymentMethod;

        @NotNull(message = "El monto a pagar es obligatorio")
        @DecimalMin(value = "0.01", message = "El monto debe ser mayor a 0")
        private BigDecimal amount;

        private BigDecimal amountTendered;
        private BigDecimal changeGiven;
        private String referenceNumber;
    }
}
