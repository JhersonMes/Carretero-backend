package com.carretero.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Opcion elegible al pedir un producto: sabor, tamano o preparacion.
 *
 * Puede colgar de un producto o de una categoria entera:
 *  - por producto: cada bebida tiene su propia lista (la limonada no comparte
 *    opciones con el emoliente aunque ambos sean BEBIDAS);
 *  - por categoria: todas las alitas de pollo comparten los mismos 9 sabores,
 *    sin importar si son de 4, 6, 8 o 10 unidades.
 * Si un producto tiene opciones propias, esas mandan sobre las de su categoria.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "flavors")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Flavor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Integer idFlavor;

    @Column(nullable = false, length = 80)
    private String name;

    /** Opciones compartidas por toda una categoria. Excluyente con {@link #product}. */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_category",
            foreignKey = @ForeignKey(name = "FK_FLAVOR_CATEGORY"))
    private Category category;

    /** Opciones propias de un solo producto. Tienen prioridad sobre las de la categoria. */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_product",
            foreignKey = @ForeignKey(name = "FK_FLAVOR_PRODUCT"))
    private Product product;

    /**
     * Cuanto suma (o resta) esta opcion al precio del producto. Sirve para casos como
     * el cafe, donde el latte cuesta S/ 1.00 mas que el americano.
     */
    @Column(name = "price_delta", nullable = false, precision = 10, scale = 2)
    private BigDecimal priceDelta = BigDecimal.ZERO;

    @Column(name = "order_index")
    private Integer orderIndex = 0;

    @Column(nullable = false)
    private boolean active = true;
}
