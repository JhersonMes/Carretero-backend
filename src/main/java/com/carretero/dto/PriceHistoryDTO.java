package com.carretero.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PriceHistoryDTO {

    private Integer idPriceHistory;
    private Integer idProduct;
    private String productName;
    private BigDecimal previousPrice;
    private BigDecimal newPrice;
    private LocalDateTime changedAt;
    private String changedByUsername;
}
