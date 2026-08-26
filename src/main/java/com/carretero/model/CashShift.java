package com.carretero.model;

import com.carretero.model.enums.CashShiftStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "cash_shifts")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class CashShift {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Integer idCashShift;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_user", nullable = false,
            foreignKey = @ForeignKey(name = "FK_CASH_SHIFT_USER"))
    private User user; // Cajero responsable del turno

    @Column(name = "open_time", nullable = false)
    private LocalDateTime openTime;

    @Column(name = "close_time")
    private LocalDateTime closeTime;

    @Column(name = "initial_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal initialAmount = BigDecimal.ZERO;

    @Column(name = "cash_sales", precision = 10, scale = 2)
    private BigDecimal cashSales = BigDecimal.ZERO;

    @Column(name = "card_sales", precision = 10, scale = 2)
    private BigDecimal cardSales = BigDecimal.ZERO;

    @Column(name = "yape_sales", precision = 10, scale = 2)
    private BigDecimal yapeSales = BigDecimal.ZERO;

    @Column(name = "plin_sales", precision = 10, scale = 2)
    private BigDecimal plinSales = BigDecimal.ZERO;

    @Column(name = "transfer_sales", precision = 10, scale = 2)
    private BigDecimal transferSales = BigDecimal.ZERO;

    @Column(name = "cash_in", precision = 10, scale = 2)
    private BigDecimal cashIn = BigDecimal.ZERO;

    @Column(name = "cash_out", precision = 10, scale = 2)
    private BigDecimal cashOut = BigDecimal.ZERO;

    @Column(name = "expected_cash", precision = 10, scale = 2)
    private BigDecimal expectedCash = BigDecimal.ZERO;

    @Column(name = "actual_cash", precision = 10, scale = 2)
    private BigDecimal actualCash = BigDecimal.ZERO;

    @Column(precision = 10, scale = 2)
    private BigDecimal difference = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CashShiftStatus status = CashShiftStatus.ABIERTA;

    @Column(length = 300)
    private String notes;

    @PrePersist
    public void prePersist() {
        if (this.openTime == null) {
            this.openTime = LocalDateTime.now();
        }
    }
}
