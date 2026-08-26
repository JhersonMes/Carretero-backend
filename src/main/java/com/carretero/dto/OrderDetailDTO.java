package com.carretero.dto;

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
    private String notes;
    private BigDecimal subtotal;
    private OrderItemStatus itemStatus = OrderItemStatus.PENDIENTE;
}
