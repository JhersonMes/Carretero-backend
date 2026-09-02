package com.carretero.model;

import com.carretero.model.enums.OrderStatus;
import com.carretero.model.enums.OrderType;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "orders", indexes = {
        @Index(name = "idx_order_created", columnList = "created_at"),
        @Index(name = "idx_order_status", columnList = "status"),
        @Index(name = "idx_order_code", columnList = "order_code")
})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Integer idOrder;

    @Column(name = "order_code", length = 30)
    private String orderCode; // ej. "PED-20260823-001"

    @Enumerated(EnumType.STRING)
    @Column(name = "sale_type", nullable = false, length = 30)
    private OrderType saleType = OrderType.SALON;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrderStatus status = OrderStatus.PENDIENTE;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_table",
            foreignKey = @ForeignKey(name = "FK_ORDER_TABLE"))
    private DiningTable table;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_user", nullable = false,
            foreignKey = @ForeignKey(name = "FK_ORDER_USER"))
    private User user; // Mesero o cajero que atendió

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_client",
            foreignKey = @ForeignKey(name = "FK_ORDER_CLIENT"))
    private Client client;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_address",
            foreignKey = @ForeignKey(name = "FK_ORDER_ADDRESS"))
    private Address deliveryAddress;

    @Column(name = "delivery_fee", precision = 10, scale = 2)
    private BigDecimal deliveryFee = BigDecimal.ZERO;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(precision = 10, scale = 2)
    private BigDecimal discount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @Column(length = 300)
    private String notes;

    @JsonManagedReference
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<OrderDetail> details = new ArrayList<>();

    /**
     * Quien anulo la venta, cuando y por que.
     *
     * Una venta anulada no se borra: la fila queda en CANCELADO con estos datos.
     * Borrarla dejaria un hueco en los correlativos del dia y haria imposible
     * explicar despues por que la caja cerro con menos de lo vendido.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_cancelled_by",
            foreignKey = @ForeignKey(name = "FK_ORDER_CANCELLED_BY"))
    private User cancelledBy;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "cancel_reason", length = 300)
    private String cancelReason;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
