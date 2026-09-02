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
@Table(name = "addresses")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Integer idAddress;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_client", nullable = false,
            foreignKey = @ForeignKey(name = "FK_ADDRESS_CLIENT"))
    private Client client;

    @Column(nullable = false, length = 150)
    private String street;

    @Column(length = 20)
    private String number;

    @Column(length = 200)
    private String reference;

    @Column(length = 100)
    private String district;

    @Column(nullable = false)
    private boolean favorite = false;

    /**
     * Lo que se cobra por llevar el pedido hasta aqui. Vive en la direccion y no
     * en el pedido porque la tarifa la fija el jiron: el mismo cliente paga
     * distinto si pide a su casa o a su trabajo, y el cajero no deberia tener
     * que acordarse del monto en cada venta.
     */
    @Column(name = "delivery_fee", precision = 10, scale = 2)
    private BigDecimal deliveryFee = BigDecimal.ZERO;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
