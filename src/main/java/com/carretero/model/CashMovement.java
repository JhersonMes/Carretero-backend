package com.carretero.model;

import com.carretero.model.enums.CashMovementType;
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
@Table(name = "cash_movements")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class CashMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Integer idMovement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cash_shift", nullable = false,
            foreignKey = @ForeignKey(name = "FK_CASH_MOVEMENT_SHIFT"))
    private CashShift cashShift;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_user", nullable = false,
            foreignKey = @ForeignKey(name = "FK_CASH_MOVEMENT_USER"))
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CashMovementType type = CashMovementType.EGRESO;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 255)
    private String reason; // ej. "Compra de servilletas de emergencia", "Sencillo inicial adicional"

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
