package com.carretero.dto;

import com.carretero.model.enums.OrderType;
import jakarta.validation.Valid;
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
public class OrderCreateRequestDTO {

    @NotNull(message = "El tipo de venta es obligatorio (SALON, DELIVERY, VENTA_RAPIDA)")
    private OrderType saleType = OrderType.SALON;

    private Integer idTable; // Obligatorio si es SALON

    private Integer idClient; // Opcional / Obligatorio para DELIVERY

    private Integer idAddress; // Para DELIVERY

    private BigDecimal deliveryFee = BigDecimal.ZERO;

    private BigDecimal discount = BigDecimal.ZERO;

    private String notes;

    @NotEmpty(message = "El pedido debe contener al menos un producto")
    @Valid
    private List<OrderItemRequestDTO> items;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemRequestDTO {
        @NotNull(message = "El id de producto es obligatorio")
        private Integer idProduct;

        @NotNull(message = "La cantidad es obligatoria")
        private Integer quantity;

        /** Opcion elegida en la pantalla de venta, si el producto la exige. */
        private Integer idFlavor;

        /** Nombre de la opcion; se guarda en el detalle para que la comanda no cambie despues. */
        private String flavorName;

        private String notes;
    }
}
