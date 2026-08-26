package com.carretero.model;

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
@Table(name = "payments", indexes = {
        @Index(name = "idx_payment_date", columnList = "payment_date")
})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Integer idPayment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_order", nullable = false,
            foreignKey = @ForeignKey(name = "FK_PAYMENT_ORDER"))
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cash_shift",
            foreignKey = @ForeignKey(name = "FK_PAYMENT_CASH_SHIFT"))
    private CashShift cashShift;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_payment_method", nullable = false,
            foreignKey = @ForeignKey(name = "FK_PAYMENT_METHOD"))
    private PaymentMethod paymentMethod;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_user", nullable = false,
            foreignKey = @ForeignKey(name = "FK_PAYMENT_USER"))
    private User user;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "amount_tendered", precision = 10, scale = 2)
    private BigDecimal amountTendered; // Monto entregado en efectivo por el cliente (ej. S/ 50.00)

    @Column(name = "change_given", precision = 10, scale = 2)
    private BigDecimal changeGiven; // Vuelto entregado (ej. S/ 12.00)

    @Column(name = "reference_number", length = 100)
    private String referenceNumber; // Nro de operación Yape/Plin o Voucher tarjeta

    @Column(name = "payment_date", nullable = false)
    private LocalDateTime paymentDate;

    @PrePersist
    public void prePersist() {
        if (this.paymentDate == null) {
            this.paymentDate = LocalDateTime.now();
        }
    }
}
