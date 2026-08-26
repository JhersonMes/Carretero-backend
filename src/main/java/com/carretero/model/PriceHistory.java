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
@Table(name = "price_histories")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class PriceHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Integer idPriceHistory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_product", nullable = false,
            foreignKey = @ForeignKey(name = "FK_PRICE_HISTORY_PRODUCT"))
    private Product product;

    @Column(name = "previous_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal previousPrice;

    @Column(name = "new_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal newPrice;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_user",
            foreignKey = @ForeignKey(name = "FK_PRICE_HISTORY_USER"))
    private User user;

    @PrePersist
    public void prePersist() {
        this.changedAt = LocalDateTime.now();
    }
}
