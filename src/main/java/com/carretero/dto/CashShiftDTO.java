package com.carretero.dto;

import com.carretero.model.enums.CashShiftStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CashShiftDTO {

    private Integer idCashShift;
    private UserDTO user;
    private LocalDateTime openTime;
    private LocalDateTime closeTime;

    private BigDecimal initialAmount;
    private BigDecimal cashSales;
    private BigDecimal cardSales;
    private BigDecimal yapeSales;
    private BigDecimal plinSales;
    private BigDecimal transferSales;

    private BigDecimal cashIn;
    private BigDecimal cashOut;

    private BigDecimal expectedCash;
    private BigDecimal actualCash;
    private BigDecimal difference;

    private CashShiftStatus status;
    private String notes;
}
