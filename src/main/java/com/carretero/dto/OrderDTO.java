package com.carretero.dto;

import com.carretero.model.enums.OrderStatus;
import com.carretero.model.enums.OrderType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderDTO {

    private Integer idOrder;
    private String orderCode;
    private OrderType saleType;
    private OrderStatus status;

    private DiningTableDTO table;
    private UserDTO user;
    private ClientDTO client;
    private AddressDTO deliveryAddress;

    private BigDecimal deliveryFee;
    private BigDecimal subtotal;
    private BigDecimal discount;
    private BigDecimal total;
    private String notes;

    private List<OrderDetailDTO> details;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
