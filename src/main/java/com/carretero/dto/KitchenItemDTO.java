package com.carretero.dto;

import com.carretero.model.enums.OrderItemStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KitchenItemDTO {

    private Integer idOrderDetail;
    private String orderCode;
    private String tableName;
    private String productName;
    private Integer quantity;
    private String flavorName;
    private String notes;
    private OrderItemStatus itemStatus;

    /** Momento en que la comanda llego a la estacion; base del cronometro en pantalla. */
    private LocalDateTime sentAt;
}
