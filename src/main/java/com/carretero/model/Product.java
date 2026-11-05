package com.carretero.model;

import com.carretero.model.enums.KitchenStation;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "products")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Integer idProduct;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 255)
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_category", nullable = false,
            foreignKey = @ForeignKey(name = "FK_PRODUCT_CATEGORY"))
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private KitchenStation station;

    @Column(name = "requires_kitchen", nullable = false)
    private boolean requiresKitchen = true;

    /** Si es true, al elegir el producto se debe pedir un sabor de su categoria. */
    @Column(name = "requires_flavor", nullable = false)
    private boolean requiresFlavor = false;

    /**
     * Si es true, el area que lo prepara se elige al pedirlo y no esta fijada
     * aqui. Es el caso de los adicionales: un platano acompana tanto a unas
     * alitas (cocina) como a una hamburguesa (parrilla), y quien sabe con cual
     * va es el mesero que toma la orden.
     *
     * Se elige una sola area por linea, no las dos: si el item saliera en las
     * dos pantallas, o lo preparan ambas o ninguna.
     */
    @Column(name = "requires_station", nullable = false)
    private boolean requiresStation = false;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "manage_stock", nullable = false)
    private boolean manageStock = false;

    @Column(precision = 10, scale = 2)
    private BigDecimal stock = BigDecimal.ZERO;

    @Column(name = "image_url", length = 300)
    private String imageUrl;
}
