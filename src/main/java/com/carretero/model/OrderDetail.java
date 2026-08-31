package com.carretero.model;

import com.carretero.model.enums.OrderItemStatus;
import com.fasterxml.jackson.annotation.JsonBackReference;
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
@Table(name = "order_details")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class OrderDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Integer idOrderDetail;

    @JsonBackReference
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_order", nullable = false,
            foreignKey = @ForeignKey(name = "FK_ORDER_DETAIL_ORDER"))
    private Order order;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_product", nullable = false,
            foreignKey = @ForeignKey(name = "FK_ORDER_DETAIL_PRODUCT"))
    private Product product;

    @Column(name = "product_name", nullable = false, length = 120)
    private String productName;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    /** Sabor elegido al pedir (ej. "BBQ Picante"). Se guarda por nombre para que la
     *  comanda historica no cambie si luego se renombra o desactiva el sabor. */
    @Column(name = "flavor_name", length = 80)
    private String flavorName;

    @Column(length = 255)
    private String notes; // ej. "sin tártara, papas bien doradas"

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_status", nullable = false, length = 30)
    private OrderItemStatus itemStatus = OrderItemStatus.EN_PREPARACION;

    /**
     * Momento en que la comanda llego a la estacion. Es propio del item y no del
     * pedido: en una mesa se agregan platos a lo largo del servicio, y cada uno
     * debe cronometrarse desde que se envio, no desde que se abrio la mesa.
     */
    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    /**
     * Momento en que la estacion marco el item como despachado. Detiene el
     * cronometro que ve el mesero en la mesa. Null mientras siga en preparacion.
     */
    @Column(name = "ready_at")
    private LocalDateTime readyAt;

    /**
     * Mesero que agrego este item. Se guarda por item y no solo en el pedido
     * porque en una misma mesa pueden intervenir varios meseros a lo largo del
     * servicio, y el salon debe mostrar a todos los que participaron.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_user",
            foreignKey = @ForeignKey(name = "FK_ORDER_DETAIL_USER"))
    private User user;
}
