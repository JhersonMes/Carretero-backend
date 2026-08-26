package com.carretero.model;

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
@Table(name = "ingredients")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Ingredient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Integer idIngredient;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 20)
    private String unitOfMeasure;

    @Column(length = 255)
    private String description;

    @Column(nullable = false)
    private boolean status;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal unitCost;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal stockQuantity;
}
