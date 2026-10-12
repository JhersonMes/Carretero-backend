package com.carretero.dto;

import com.carretero.model.enums.KitchenStation;
import com.carretero.model.enums.OrderItemStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderDetailDTO {

    private Integer idOrderDetail;
    private Integer idProduct;
    private String productName;
    private Integer quantity;
    private BigDecimal unitPrice;
    private String flavorName;
    /** Area elegida para esta linea; null significa la del producto. */
    private KitchenStation station;

    /**
     * Area propia del producto. Viaja aparte de `station` para que la pantalla
     * pueda saber a donde sale realmente cada plato sin tener que cargar la
     * carta entera: el area efectiva es `station` si esta, y esta si no.
     */
    private KitchenStation productStation;
    private String notes;
    private BigDecimal subtotal;
    private OrderItemStatus itemStatus = OrderItemStatus.PENDIENTE;
}
